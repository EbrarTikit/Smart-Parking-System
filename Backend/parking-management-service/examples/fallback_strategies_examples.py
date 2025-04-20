"""
Fallback Strategies Examples

This file contains practical examples of the fallback strategies mentioned in the README.
Each strategy is implemented as a separate function with sample usage.
"""

import time
import random
import requests
from functools import wraps
from typing import Callable, Any, Dict, List, Optional, Union
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

#################################
# 1. Retry with Backoff Strategy
#################################

def retry_with_backoff(max_retries: int = 3, backoff_factor: float = 2.0):
    """
    Decorator for implementing the Retry with Backoff strategy.
    
    Args:
        max_retries: Maximum number of retry attempts
        backoff_factor: Multiplier for the delay between retries
    """
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            retries = 0
            delay = 1  # Initial delay in seconds
            
            while retries < max_retries:
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    retries += 1
                    if retries == max_retries:
                        logger.error(f"All retry attempts failed: {str(e)}")
                        raise
                    
                    wait_time = delay * (backoff_factor ** (retries - 1))
                    logger.warning(f"Attempt {retries} failed: {str(e)}. Retrying in {wait_time:.2f} seconds...")
                    time.sleep(wait_time)
            
            # Should not reach here
            return None
        return wrapper
    return decorator

# Example usage of Retry with Backoff
@retry_with_backoff(max_retries=4, backoff_factor=2)
def unreliable_license_plate_verification(plate_number: str) -> bool:
    """Simulates a flaky license plate verification service."""
    # Simulate network or service failure
    if random.random() < 0.7:  # 70% chance of failure on first tries
        logger.info(f"License plate verification service is experiencing issues...")
        raise ConnectionError("Failed to connect to the verification service")
    
    # If no exception, verification succeeded
    logger.info(f"Successfully verified license plate: {plate_number}")
    return True

def demonstrate_retry_with_backoff():
    """Demonstrates the Retry with Backoff strategy."""
    print("\n=== DEMONSTRATING RETRY WITH BACKOFF STRATEGY ===")
    try:
        result = unreliable_license_plate_verification("34ABC123")
        print(f"Final result: {result}")
    except Exception as e:
        print(f"Operation ultimately failed: {str(e)}")

#################################
# 2. Circuit Breaker Strategy
#################################

class CircuitBreaker:
    """
    Implementation of the Circuit Breaker pattern.
    
    The circuit breaker has three states:
    - CLOSED: All requests pass through
    - OPEN: Requests immediately fail without attempting to call the service
    - HALF-OPEN: Limited number of requests pass through to test if the service is recovered
    """
    
    # Circuit states
    CLOSED = 'CLOSED'
    OPEN = 'OPEN'
    HALF_OPEN = 'HALF_OPEN'
    
    def __init__(self, failure_threshold: int = 5, recovery_timeout: int = 30, 
                 half_open_max_calls: int = 3):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.half_open_max_calls = half_open_max_calls
        
        self.state = self.CLOSED
        self.failure_count = 0
        self.last_failure_time = None
        self.half_open_calls = 0
    
    def __call__(self, func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            if self.state == self.OPEN:
                if time.time() - self.last_failure_time > self.recovery_timeout:
                    logger.info("Circuit moving to HALF-OPEN state")
                    self.state = self.HALF_OPEN
                    self.half_open_calls = 0
                else:
                    raise CircuitBreakerOpenError(f"Circuit is OPEN, service calls blocked until {self.recovery_timeout - (time.time() - self.last_failure_time):.2f} more seconds")
            
            if self.state == self.HALF_OPEN and self.half_open_calls >= self.half_open_max_calls:
                raise CircuitBreakerOpenError("Maximum test calls reached in HALF-OPEN state")
            
            if self.state == self.HALF_OPEN:
                self.half_open_calls += 1
            
            try:
                result = func(*args, **kwargs)
                
                # Success - reset or improve state
                if self.state == self.HALF_OPEN:
                    logger.info("Service recovered, circuit moving to CLOSED state")
                    self.reset()
                
                return result
                
            except Exception as e:
                # Failure - worsen state
                self.record_failure()
                
                if self.state == self.CLOSED and self.failure_count >= self.failure_threshold:
                    logger.warning(f"Failure threshold reached, circuit moving to OPEN state for {self.recovery_timeout} seconds")
                    self.state = self.OPEN
                    self.last_failure_time = time.time()
                
                if self.state == self.HALF_OPEN:
                    logger.warning("Test call failed in HALF-OPEN state, circuit moving back to OPEN state")
                    self.state = self.OPEN
                    self.last_failure_time = time.time()
                
                raise e
        
        return wrapper
    
    def record_failure(self):
        """Records a failure and updates the failure count."""
        self.failure_count += 1
    
    def reset(self):
        """Resets the circuit breaker to the initial closed state."""
        self.state = self.CLOSED
        self.failure_count = 0
        self.last_failure_time = None
        self.half_open_calls = 0

class CircuitBreakerOpenError(Exception):
    """Exception raised when the circuit is open."""
    pass

# Example usage of Circuit Breaker
payment_service_circuit = CircuitBreaker(failure_threshold=3, recovery_timeout=10, half_open_max_calls=2)

@payment_service_circuit
def process_parking_payment(transaction_id: str, amount: float) -> bool:
    """Simulates a payment processing service with potential failures."""
    # Simulate a service that fails intermittently
    if random.random() < 0.6:  # 60% chance of failure
        logger.error("Payment service is down or experiencing issues")
        raise ConnectionError("Failed to connect to payment service")
    
    logger.info(f"Successfully processed payment of ${amount:.2f} for transaction {transaction_id}")
    return True

def demonstrate_circuit_breaker():
    """Demonstrates the Circuit Breaker strategy."""
    print("\n=== DEMONSTRATING CIRCUIT BREAKER STRATEGY ===")
    
    for i in range(10):
        try:
            transaction_id = f"TXN-{random.randint(1000, 9999)}"
            result = process_parking_payment(transaction_id, 15.00)
            print(f"Payment attempt {i+1}: Success")
        except CircuitBreakerOpenError as e:
            print(f"Payment attempt {i+1}: Circuit is open - {str(e)}")
        except Exception as e:
            print(f"Payment attempt {i+1}: Failed - {str(e)}")
        
        time.sleep(1)  # Wait between attempts

#################################
# 3. Alternative Service Strategy
#################################

class ServiceUnavailableError(Exception):
    """Exception raised when a service is unavailable."""
    pass

def with_alternative_service(alternative_func: Callable):
    """
    Decorator for implementing the Alternative Service strategy.
    
    Args:
        alternative_func: Function to call as a fallback if the primary service fails
    """
    def decorator(primary_func):
        @wraps(primary_func)
        def wrapper(*args, **kwargs):
            try:
                return primary_func(*args, **kwargs)
            except Exception as e:
                logger.warning(f"Primary service failed: {str(e)}. Trying alternative service...")
                return alternative_func(*args, **kwargs)
        return wrapper
    return decorator

# Primary and alternative parking space allocation services
def primary_space_allocation_service(vehicle_type: str) -> Dict:
    """Primary service for parking space allocation."""
    # Simulate service failure
    if random.random() < 0.7:  # 70% chance of failure
        logger.error("Primary parking space allocation service is down")
        raise ServiceUnavailableError("Primary parking space allocation service unavailable")
    
    # Simulate successful allocation
    space_id = f"A-{random.randint(1, 100)}"
    logger.info(f"Primary service allocated space {space_id} for {vehicle_type}")
    return {"space_id": space_id, "level": 1, "section": "A"}

def alternative_space_allocation_service(vehicle_type: str) -> Dict:
    """Alternative service for parking space allocation."""
    # This service has a higher success rate but allocates spaces from a different section
    if random.random() < 0.2:  # Only 20% chance of failure
        logger.error("Alternative parking space allocation service is also down")
        raise ServiceUnavailableError("Alternative parking space allocation service unavailable")
    
    # Allocate from backup section
    space_id = f"B-{random.randint(1, 100)}"
    logger.info(f"Alternative service allocated space {space_id} for {vehicle_type}")
    return {"space_id": space_id, "level": 2, "section": "B"}

@with_alternative_service(alternative_space_allocation_service)
def allocate_parking_space(vehicle_type: str) -> Dict:
    """Allocates a parking space using primary service with fallback to alternative."""
    return primary_space_allocation_service(vehicle_type)

def demonstrate_alternative_service():
    """Demonstrates the Alternative Service strategy."""
    print("\n=== DEMONSTRATING ALTERNATIVE SERVICE STRATEGY ===")
    
    vehicle_types = ["Sedan", "SUV", "Truck", "Motorcycle", "Van"]
    
    for i in range(5):
        try:
            vehicle_type = random.choice(vehicle_types)
            space = allocate_parking_space(vehicle_type)
            print(f"Attempt {i+1}: Successfully allocated space {space['space_id']} in section {space['section']} for {vehicle_type}")
        except Exception as e:
            print(f"Attempt {i+1}: Both primary and alternative services failed - {str(e)}")

#################################
# 4. Cache Fallback Strategy
#################################

class CacheFallback:
    """
    Implementation of the Cache Fallback strategy.
    
    This class caches successful responses from a service and uses them as a fallback
    when the service is unavailable.
    """
    
    def __init__(self, cache_ttl: int = 3600):
        """
        Initialize the cache fallback mechanism.
        
        Args:
            cache_ttl: Time-to-live for cache entries in seconds (default 1 hour)
        """
        self.cache = {}
        self.cache_timestamps = {}
        self.cache_ttl = cache_ttl
    
    def __call__(self, func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            # Create a cache key from function name and arguments
            key = self._create_cache_key(func.__name__, args, kwargs)
            
            try:
                # Try to call the service
                result = func(*args, **kwargs)
                
                # Cache the successful result
                self._update_cache(key, result)
                
                return result
                
            except Exception as e:
                logger.warning(f"Service call failed: {str(e)}. Checking cache...")
                
                # Check if we have a valid cached value
                cached_value = self._get_from_cache(key)
                if cached_value is not None:
                    logger.info(f"Using cached value for {func.__name__}")
                    return cached_value
                
                # Re-raise the exception if no valid cache entry exists
                logger.error(f"No valid cache entry found for {func.__name__}")
                raise
        
        return wrapper
    
    def _create_cache_key(self, func_name: str, args: tuple, kwargs: dict) -> str:
        """Creates a unique key for the cache based on function name and arguments."""
        # Simple implementation - in production you might want a more sophisticated approach
        arg_str = '-'.join(str(arg) for arg in args)
        kwarg_str = '-'.join(f"{k}:{v}" for k, v in sorted(kwargs.items()))
        return f"{func_name}|{arg_str}|{kwarg_str}"
    
    def _update_cache(self, key: str, value: Any) -> None:
        """Updates the cache with a new value."""
        self.cache[key] = value
        self.cache_timestamps[key] = time.time()
        
        # Clean up old cache entries
        self._clean_cache()
    
    def _get_from_cache(self, key: str) -> Optional[Any]:
        """Retrieves a value from the cache if it exists and is still valid."""
        if key in self.cache:
            timestamp = self.cache_timestamps.get(key, 0)
            if time.time() - timestamp <= self.cache_ttl:
                return self.cache[key]
        
        return None
    
    def _clean_cache(self) -> None:
        """Removes expired entries from the cache."""
        current_time = time.time()
        expired_keys = [
            key for key, timestamp in self.cache_timestamps.items() 
            if current_time - timestamp > self.cache_ttl
        ]
        
        for key in expired_keys:
            del self.cache[key]
            del self.cache_timestamps[key]

# Example usage of Cache Fallback
vehicle_info_cache = CacheFallback(cache_ttl=30)  # Cache for 30 seconds

@vehicle_info_cache
def get_vehicle_info(plate_number: str) -> Dict:
    """Get vehicle information from an external service."""
    # Simulate external service call
    if random.random() < 0.6:  # 60% chance of failure
        logger.error(f"Vehicle information service is unavailable")
        raise ConnectionError("Failed to connect to vehicle information service")
    
    # Simulate successful response
    vehicle_types = ["Sedan", "SUV", "Truck", "Motorcycle", "Van"]
    vehicle_info = {
        "plate": plate_number,
        "type": random.choice(vehicle_types),
        "registered_owner": f"Owner-{random.randint(1000, 9999)}",
        "last_updated": time.strftime("%Y-%m-%d %H:%M:%S")
    }
    
    logger.info(f"Retrieved vehicle info for plate {plate_number}")
    return vehicle_info

def demonstrate_cache_fallback():
    """Demonstrates the Cache Fallback strategy."""
    print("\n=== DEMONSTRATING CACHE FALLBACK STRATEGY ===")
    
    plate_numbers = ["34ABC123", "06XYZ789", "35DEF456", "34GHI789", "01JKL321"]
    
    for i in range(10):
        try:
            plate = random.choice(plate_numbers)
            info = get_vehicle_info(plate)
            print(f"Attempt {i+1}: Got info for {plate} - Type: {info['type']}, Owner: {info['registered_owner']}, Updated: {info['last_updated']}")
        except Exception as e:
            print(f"Attempt {i+1}: Failed completely - {str(e)}")
        
        time.sleep(2)  # Space out the requests

#################################
# 5. Load Balancing Strategy
#################################

class LoadBalancer:
    """
    Simple round-robin load balancer implementation.
    
    This load balancer distributes requests across multiple service instances.
    When one service fails, it tries the next available service.
    """
    
    def __init__(self, services: List[Callable]):
        """
        Initialize the load balancer with a list of service functions.
        
        Args:
            services: List of service functions to balance between
        """
        if not services:
            raise ValueError("At least one service must be provided")
        
        self.services = services
        self.current_index = 0
        self.service_statuses = [True] * len(services)  # All services start as healthy
    
    def call(self, *args, **kwargs) -> Any:
        """
        Call the next available service.
        
        This method implements the round-robin selection with failover.
        """
        # Record the starting index to detect if we've tried all services
        start_index = self.current_index
        
        while True:
            # Get the current service
            current_service = self.services[self.current_index]
            
            # Move to the next service for the next call
            self.current_index = (self.current_index + 1) % len(self.services)
            
            # Skip if the service is marked as unhealthy
            if not self.service_statuses[self.current_index - 1]:
                # If we've gone full circle, all services are down
                if self.current_index == start_index:
                    raise Exception("All services are unavailable")
                continue
            
            try:
                # Attempt to call the service
                return current_service(*args, **kwargs)
            except Exception as e:
                # Mark the service as unhealthy
                self.service_statuses[self.current_index - 1] = False
                logger.warning(f"Service {self.current_index - 1} failed: {str(e)}")
                
                # Schedule a health check after some time
                self._schedule_health_check(self.current_index - 1)
                
                # If we've gone full circle, all services are down
                if self.current_index == start_index:
                    raise Exception("All services are unavailable")
    
    def _schedule_health_check(self, service_index: int) -> None:
        """
        Schedules a service to be marked healthy again after a timeout.
        
        In a real implementation, this would perform actual health checks.
        Here, we simply mark the service as healthy after a delay.
        """
        def mark_healthy():
            logger.info(f"Marking service {service_index} as healthy again")
            self.service_statuses[service_index] = True
        
        # In a real implementation, this would start a separate thread or use a task scheduler
        # For this example, we'll just wait 5 seconds and then mark it healthy again
        import threading
        threading.Timer(5.0, mark_healthy).start()

# Example service implementations for load balancing
def parking_fee_service_1(duration_hours: float) -> float:
    """Calculate parking fee using the first service."""
    if random.random() < 0.4:  # 40% chance of failure
        logger.error("Parking fee service 1 is experiencing issues")
        raise ConnectionError("Failed to connect to parking fee service 1")
    
    # Base rate plus hourly rate
    fee = 5.00 + (duration_hours * 2.50)
    logger.info(f"Service 1 calculated fee: ${fee:.2f} for {duration_hours} hours")
    return fee

def parking_fee_service_2(duration_hours: float) -> float:
    """Calculate parking fee using the second service."""
    if random.random() < 0.4:  # 40% chance of failure
        logger.error("Parking fee service 2 is experiencing issues")
        raise ConnectionError("Failed to connect to parking fee service 2")
    
    # Flat hourly rate with minimum
    fee = max(7.50, duration_hours * 3.00)
    logger.info(f"Service 2 calculated fee: ${fee:.2f} for {duration_hours} hours")
    return fee

def parking_fee_service_3(duration_hours: float) -> float:
    """Calculate parking fee using the third service."""
    if random.random() < 0.4:  # 40% chance of failure
        logger.error("Parking fee service 3 is experiencing issues")
        raise ConnectionError("Failed to connect to parking fee service 3")
    
    # Progressive rate (higher for longer stays)
    if duration_hours <= 2:
        fee = duration_hours * 2.00
    else:
        fee = 4.00 + ((duration_hours - 2) * 3.50)
    
    logger.info(f"Service 3 calculated fee: ${fee:.2f} for {duration_hours} hours")
    return fee

# Create a load balancer for the parking fee services
parking_fee_load_balancer = LoadBalancer([
    parking_fee_service_1,
    parking_fee_service_2,
    parking_fee_service_3
])

def calculate_parking_fee(duration_hours: float) -> float:
    """Calculate parking fee using load-balanced services."""
    return parking_fee_load_balancer.call(duration_hours)

def demonstrate_load_balancing():
    """Demonstrates the Load Balancing strategy."""
    print("\n=== DEMONSTRATING LOAD BALANCING STRATEGY ===")
    
    durations = [1.5, 3.0, 4.5, 2.0, 5.5, 0.5, 6.0]
    
    for i in range(10):
        try:
            duration = random.choice(durations)
            fee = calculate_parking_fee(duration)
            print(f"Attempt {i+1}: Calculated fee of ${fee:.2f} for {duration} hours")
        except Exception as e:
            print(f"Attempt {i+1}: All services failed - {str(e)}")
        
        time.sleep(1)  # Space out the requests

#################################
# Main demonstration function
#################################

def main():
    """Run demonstrations of all fallback strategies."""
    print("=" * 80)
    print("FALLBACK STRATEGIES DEMONSTRATION")
    print("=" * 80)
    
    # Demonstrate each strategy
    demonstrate_retry_with_backoff()
    demonstrate_circuit_breaker()
    demonstrate_alternative_service()
    demonstrate_cache_fallback()
    demonstrate_load_balancing()
    
    print("\nAll fallback strategy demonstrations completed!")

if __name__ == "__main__":
    main() 