# Smart Parking System

A modern and intelligent parking management solution that helps users find and monitor parking spots in real-time.

## 🌟 Features

- **Real-time Parking Spot Tracking**
  - Live map view of available parking spots
  - Color-coded indicators (green for available, red for occupied)
  - Real-time updates on spot availability
  - Google Maps integration
  - Location-based parking spot search
  - Interactive map markers and clustering

- **User-Friendly Interface**
  - Modern Material Design implementation
  - Dark mode support
  - Intuitive navigation
  - Responsive layout
  - Smooth animations and transitions

- **Personalization**
  - User profiles with preferences
  - Favorite parking spots
  - Customizable notification settings
  - User activity tracking

- **Push Notifications**
  - Real-time parking spot availability alerts
  - Custom notification channels
  - Background notification handling
  - Notification preferences management
  - Firebase Cloud Messaging (FCM) integration

- **AI-Powered Assistant**
  - Smart chatbot for user support
  - Quick answers to common questions
  - Parking guidance and recommendations

## 🛠 Technical Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Component
- **UI Components**: Material Design
- **Asynchronous Operations**: Coroutines & Flow
- **Image Loading**: Glide
- **Network**: 
  - Retrofit for REST API
  - OkHttp for HTTP client
  - WebSocket for real-time communication
  - Moshi for JSON parsing
- **Location Services**: 
  - Google Maps SDK
  - Google Play Services Location
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Testing**:
  - JUnit for unit testing
  - Mockk for mocking
  - Espresso for UI testing
  - Truth for assertions
  - Hilt for dependency injection testing
- **Animation**: Lottie for animations
- **Security**: Network Security Configuration
- **Build Tools**:
  - Gradle with Kotlin DSL
  - Safe Args for Navigation
  - Kotlin Parcelize

## App Design
<!-- First Row -->
<p align="center">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/signup.png" alt="Image 1" width="30%">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/signin.png" alt="Image 2" width="30%">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/locationpermission.png" alt="Image 3" width="30%">
</p>

<!-- Second Row -->
<p align="center">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/home.png" alt="Image 4" width="30%">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/detail.png" alt="Image 5" width="30%">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/layout.png" alt="Image 6" width="30%">
</p>

<!-- Second Row -->
<p align="center">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/chatbotopening.png" alt="Image 7" width="30%">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/chatbot.png" alt="Image 8" width="30%">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/profile.png" alt="Image 9" width="30%">
</p>

<!-- Second Row -->
<p align="center">
  <img src="https://github.com/EbrarTikit/Smart-Parking-System/blob/master/Frontend/screenshots/darkmode.png" alt="Image 10" width="30%">
</p>

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 11 or newer
- Android SDK 21 or newer
- Google Maps API Key

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/EbrarTikit/Smart-Parking-System
   ```

2. Open the project in Android Studio

3. Add your Google Maps API key in `secrets`:
   ```properties
   MAPS_API_KEY=your_api_key_here
   ```

4. Build and run the application

## 🔧 Configuration

### Environment Setup

1. Enable Google Maps SDK for Android
2. Configure Firebase project
3. Set up Google Cloud Console project
4. Enable necessary APIs in Google Cloud Console

## 📦 Project Structure

```
app/
├── data/
│   ├── model/         # Data models
│   ├── repository/    # Repository implementations
│   └── source/        # Data sources (local & remote)
├── di/                # Dependency injection modules
├── ui/                # UI components
│   ├── auth/         # Authentication screens
│   ├── home/         # Home screen
│   ├── profile/      # Profile management
│   └── common/       # Shared UI components
└── utils/            # Utility classes
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- Ebrar Tikit - Android Developer - [MyGitHub](https://github.com/EbrarTikit)

## 🙏 Acknowledgments

- Material Design Components
- Google Maps Platform
- Firebase
- Android Jetpack Libraries

## 📞 Support

For support, email [tikitebrar@gmail.com](mailto:tikitebrar@gmail.com)

## 🔄 Updates

### Version 1.0.0
- Initial release
- Real-time parking spot availability tracking
- User authentication
- Google Maps integration
- Interactive parking spot visualization
- Push notification system with FCM

### Version 1.1.0 (Coming Soon)
- Enhanced user experience
- Advanced analytics
- Multi-language support
- Offline mode
- Enhanced security features
- Advanced notification customization 
