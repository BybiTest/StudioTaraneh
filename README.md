# Studio Taraneh 1.5.0

## وضعیت این نسخه
نسخه 1.5.0 بر پایه v13 ساخته شده و هسته Local-First را حفظ می‌کند.

### اصلاحات و تکمیل‌های این نسخه
- Theme Engine واقعی برای Neon Studio، Midnight، Graphite، Purple Night و AMOLED
- حالت نمایش روشن / تاریک / طبق سیستم
- اعمال سراسری اندازه فونت
- صفحه مستقل علاقه‌مندی‌ها
- صفحه مستقل اخیر
- ثبت واقعی زمان آخرین بازشدن ترانه با Room Migration 6→7
- صفحات About، Privacy و Terms
- نمایش نسخه 1.5.0 در Splash و Home
- ثبت آیکون برنامه در Manifest
- مدیریت واقعی سبک‌ها و زیرسبک‌های پیش‌فرض و سفارشی با Room و Migration 7→8
- Tap Tempo واقعی
- مترونوم مستقل داخل ریتم‌ساز
- خروجی Backup با پسوند اختصاصی `.taraneh` (ساختار ZIP استاندارد برای سازگاری) و نگهداری سبک‌های سفارشی
- حفظ TXT/JSON، Share، Recording/Take، Version History، Timeline، Drum Pattern و Zip Slip protection
- آماده‌سازی امضای Release در GitHub Actions با Secrets اختیاری

## قابلیت‌هایی که هنوز به اطلاعات/سرویس خارجی نیاز دارند
- Tapsell: نیازمند App ID/Placementهای واقعی صاحب برنامه است؛ SDK جعلی یا تبلیغ نمایشی اضافه نشده است.
- VIP و خرید کافه‌بازار: نیازمند شناسه محصول، کلید/پیکربندی حساب توسعه‌دهنده و تنظیمات واقعی کافه‌بازار است؛ خرید نمایشی اضافه نشده است.
- ترجمه کامل انگلیسی/عربی و زبان‌های دیگر هنوز باید UI را از متن‌های hard-coded به منابع localization منتقل کند؛ گزینه زبان فعلاً فقط در تنظیمات ذخیره می‌شود و نباید به‌عنوان ترجمه کامل تلقی شود.

## Build
GitHub Actions شامل Debug APK و Release APK/AAB است.
برای Release امضاشده، این Secrets را در GitHub Repository تنظیم کنید:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

اگر Secrets تنظیم نشده باشند، Workflow همچنان می‌تواند خروجی Release بدون امضا تولید کند.

## نسخه
- versionCode: 15
- versionName: 1.5.0

## محدودیت محیط فعلی
در محیط فعلی Gradle و اینترنت خروجی در دسترس نبود، بنابراین Build نهایی APK/AAB از این محیط قابل تأیید نبود. ساخت واقعی در GitHub Actions پروژه انجام می‌شود.
