# LightShare
LightShare is an offline Android file-sharing application that transfers files between smartphones using animated QR codes and fountain coding, without requiring internet, Wi-Fi, Bluetooth, or cloud services. Built with Kotlin and Jetpack Compose.
# LightShare 📱✨
### Offline File Transfer Using Animated QR Codes

LightShare is an Android application designed to transfer files between two smartphones without relying on internet connectivity, Wi-Fi, Bluetooth, or cloud infrastructure.

It uses **animated QR codes and Fountain Coding** to transmit file data optically from one device to another. The sender displays continuously changing QR codes, while the receiver scans them using its camera and reconstructs the original file.

## 🚀 Key Features

- **Offline File Transfer:** Exchange files without internet, Wi-Fi, or Bluetooth.
- **Animated QR Transmission:** Convert file data into continuously changing QR codes.
- **Fountain Coding:** Use redundant encoded packets to support recovery from missed QR frames.
- **Real-Time QR Scanning:** Capture QR codes using the smartphone camera.
- **File Reconstruction:** Decode received packets and rebuild the original file.
- **Data Integrity:** Verify reconstructed files using SHA-256 checksums.
- **Transfer Monitoring:** Track packet reception, transmission speed, and transfer progress.
- **Modern Android UI:** Built using Jetpack Compose and Material 3.

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Jetpack Compose | Modern Android UI |
| Android Studio | Development environment |
| MVVM | Application architecture |
| CameraX | Camera integration |
| Google ML Kit | QR code detection |
| ZXing | QR code generation |
| Kotlin Coroutines | Asynchronous processing |
| LT Fountain Codes | Redundant data encoding and recovery |
| SHA-256 | File integrity verification |
| Android Storage Access Framework | File selection and saving |

## ⚙️ How It Works

1. **File Selection:** The sender selects a file from Android storage.
2. **Data Chunking:** The file is divided into smaller data blocks.
3. **Fountain Encoding:** Data blocks are encoded into redundant packets.
4. **QR Generation:** Encoded packets are converted into QR codes.
5. **Optical Transmission:** The sender displays QR codes sequentially.
6. **QR Scanning:** The receiver captures QR codes using its camera.
7. **Packet Recovery:** Received packets are processed to recover the original data.
8. **File Reconstruction:** The original file is reconstructed and verified using SHA-256.

## 🏗️ Architecture

The application follows a modular MVVM architecture with separate components for:

- Presentation Layer
- Sender Module
- Receiver Module
- QR Generation and Scanning
- Fountain Encoding and Decoding
- File Management
- Data Integrity Verification

## 📱 Platform Requirements

- Android smartphones
- Android Studio
- Camera permission for receiving files
- Same compatible LightShare application installed on both devices

## 🎯 Project Objective

The primary objective of LightShare is to explore an alternative approach to wireless file transfer using optical communication, QR encoding, and error-tolerant data transmission techniques.

The project combines Android development, data encoding algorithms, computer vision, and communication protocol design into a single application.

## 🔮 Future Enhancements

- Optimize QR transmission speed.
- Support larger file transfers.
- Improve decoding reliability using advanced fountain-code techniques.
- Add optional end-to-end encryption.
- Improve scanning under different lighting conditions.
- Introduce adaptive QR frame rates.
- Optimize performance across different Android devices.

## 👨‍💻 Development Status

🚧 Under Development

The project is being developed using Kotlin and Jetpack Compose, with a focus on reliable offline file transmission and efficient QR-based data recovery.

---

**LightShare — Transfer Files Through Light, Not Networks.**
