# Firebase Crashlytics - Tự động bắt crashes

## ✅ Setup hoàn tất!

Firebase Crashlytics đã được cấu hình để **TỰ ĐỘNG** bắt và báo cáo crashes mà không cần try-catch.

## 🚀 Cách hoạt động

1. **App crash** → Firebase tự động detect
2. **Thu thập thông tin** → Stack trace, device info, app version
3. **Gửi report** → Tự động upload lên Firebase Console
4. **Nhận thông báo** → Email alert khi có crash

## 📊 Xem crash reports

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Chọn project **"sieughichu"**
3. Vào mục **Crashlytics**
4. Xem crashes, affected users, trends

## 🧪 Test crash reporting

Thêm code này vào bất kỳ đâu để test:

```kotlin
// Test crash
throw RuntimeException("Test crash for Firebase")

// Hoặc null pointer exception
val test: String? = null
val length = test!!.length
```

## 🔧 Build commands

```bash
# Build debug APK
.\gradlew assembleSieuGhiChuDebug

# Build release APK  
.\gradlew assembleSieuGhiChuRelease

# Install debug
.\gradlew installSieuGhiChuDebug
```

## ⚠️ Lưu ý quan trọng

- **KHÔNG dùng try-catch** nếu muốn Firebase tự động bắt crashes
- **Debug builds** có thể có delay trong reporting
- **Release builds** sẽ report crashes ngay lập tức
- **Internet connection** cần thiết để gửi reports

## 🎯 Kết quả

App sẽ tự động gửi crash reports về Firebase Console mà không cần can thiệp thủ công!

### Files đã tạo/cập nhật:
- `gradle/libs.versions.toml` - Firebase dependencies
- `app/build.gradle.kts` - Plugins và dependencies
- `app/google-services.json` - Firebase config
- `MyApplication.kt` - Khởi tạo Crashlytics
- `AndroidManifest.xml` - Đăng ký Application class