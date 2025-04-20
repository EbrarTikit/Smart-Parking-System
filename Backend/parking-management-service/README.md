# Parking Management Service

The Parking Management Service is the central component of the smart parking system. This service manages all processes from a vehicle's entry to exit from the parking lot and ensures coordination by communicating with other services.

## Features

- Vehicle entry/exit management
- Parking space allocation
- Distributed transaction management with SAGA pattern
- Fault tolerance with fallback strategies
- Telemetry and monitoring support

## SAGA Pattern

The SAGA pattern is a design pattern used to manage distributed transactions across multiple services. Each transaction step is executed in a separate service, and if a step fails, compensation actions are performed for previously completed steps.

### SAGA Approaches

1. **Choreography-Based SAGA**: Services directly communicate with each other to execute SAGA transactions.
2. **Orchestration-Based SAGA**: A central orchestrator coordinates all transaction steps and initiates compensation actions when necessary.

In this project, we use the Orchestration-Based SAGA approach.

### SAGA Flows in Our Project

1. **Vehicle Entry Flow**:

   - License plate verification (License Plate Recognition Service)
   - Parking space allocation (Parking Management Service)
   - Entry record creation (License Plate Recognition Service)

2. **Vehicle Exit Flow**:
   - License plate verification (License Plate Recognition Service)
   - Parking space release (Parking Management Service)
   - Exit record creation (License Plate Recognition Service)

## Fallback Strategies

Strategies used when a service call fails during SAGA transactions:

### 1. Retry with Backoff

A strategy to retry a failed service call with a specific delay. The delay period increases after each unsuccessful attempt.

```python
# Example usage
success, result = await retry_with_backoff(
    call_service,
    retry_count=3,
    initial_delay=0.5,
    backoff_factor=2.0
)
```

### 2. Circuit Breaker

Used to protect a service by blocking calls to it for a certain period if it continuously returns errors.

```python
# Example usage
circuit_breaker = CircuitBreaker("license_plate_service", failure_threshold=5)
success, result = await circuit_breaker.execute(service_func, operation, request_data)
```

### 3. Alternative Service

Redirects to an alternative service that provides the same functionality when a service is unavailable.

```python
# Example usage
alt_strategy = AlternativeServiceStrategy("primary_service", ["backup_service1", "backup_service2"])
success, result = await alt_strategy.execute(primary_func, alternative_funcs)
```

### 4. Cache Fallback

Uses previously cached results when a service is unavailable.

```python
# Example usage
cache_strategy = CacheFallbackStrategy(cache_ttl_seconds=300)
success, result = await cache_strategy.execute(call_service, cache_key="unique_key")
```

### 5. Load Balancing

Ensures service continuity by distributing load across multiple service instances.

```python
# Example usage
lb_strategy = LoadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
service = lb_strategy.get_service([service1, service2, service3])
result = await service(operation, data)
```

## Example SAGA Implementation

The following example shows a SAGA implementation enhanced with fallback strategies:

```python
# Create SAGA
saga = EnhancedSaga(str(uuid.uuid4()), "Vehicle Entry Process")

# 1. License plate verification step
verify_step = EnhancedSagaStep("verify_license_plate", "license_plate_service", "verify_license_plate")
verify_step.request_data = {"license_plate": "34ABC123"}
verify_step.retry_count = 2
verify_step.use_cache_fallback = True  # Return from cache if failed
saga.add_step(verify_step)

# 2. Parking space allocation step
allocate_step = EnhancedSagaStep("allocate_parking_space", "parking_mgmt_service", "allocate_parking_space")
allocate_step.request_data = {"license_plate": "34ABC123"}
allocate_step.retry_count = 3
allocate_step.use_circuit_breaker = True  # Use circuit breaker
saga.add_step(allocate_step)

# 3. Entry record creation step
entry_step = EnhancedSagaStep("create_entry_record", "license_plate_service", "create_entry_record")
entry_step.request_data = {"license_plate": "34ABC123"}
entry_step.use_circuit_breaker = True
entry_step.alternative_services = ["alternative_license_plate_service"]  # Add alternative service
saga.add_step(entry_step)

# Execute SAGA
orchestrator = EnhancedSagaOrchestrator()
result_saga = await orchestrator.execute_saga(saga)
```

## Compensation Actions

If any step fails during a SAGA transaction, compensation actions are performed for steps that have already been completed successfully. These actions are executed in exactly reverse order (from end to beginning).

Example compensation actions:

1. Compensation for **parking space allocation**: Release the parking space
2. Compensation for **entry record creation**: Cancel the entry record

## Testing

To run our example files:

```bash
# Run the fallback strategies example
python -m app.sagas.fallback_strategies

# Run the SAGA example
python -m app.sagas.saga_with_fallbacks
```

## Installation

```bash
# Install dependencies
pip install -r requirements.txt

# Run with Docker
docker build -t parking-management-service .
docker run -p 8000:8000 parking-management-service
```
