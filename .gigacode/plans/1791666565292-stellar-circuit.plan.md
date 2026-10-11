# Implementation Plan: Christmas Trees Map Feature

## Summary
Добавить карту с геолокацией, возможность ставить маркеры (ёлки, снеговики, украшения) и список всех размещённых объектов.

**Два экрана**: MapScreen (карта с маркерами) + ListScreen (список по категориям)
**Стек**: Google Maps Compose SDK, Room, Navigation Compose, ViewModel + StateFlow

---

## Step 1: Зависимости

### `gradle/libs.versions.toml` — добавить в [versions]:
```
mapsCompose = "2.15.0"
navigationCompose = "2.8.9"
room = "2.6.1"
ksp = "2.0.21-1.0.28"
playServicesLocation = "21.3.0"
```

### `gradle/libs.versions.toml` — добавить в [libraries]:
```
androidx-maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "mapsCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
```

### `gradle/libs.versions.toml` — добавить в [plugins]:
```
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### `app/build.gradle.kts` — добавить плагин:
```kotlin
alias(libs.plugins.ksp)
```

### `app/build.gradle.kts` — добавить зависимости:
```kotlin
implementation(libs.androidx.maps.compose)
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
implementation(libs.androidx.play.services.location)
```

### `AndroidManifest.xml` — добавить разрешения:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Step 2: Data Layer (Room)

### Новые файлы:

1. **`data/MarkerType.kt`** — enum с типами маркеров (🌲 Christmas Tree, ☃️ Snowman, 🎄 City Decoration, ⭐ Star, 🎁 Gift) + категории (TREES, SNOWMEN, DECORATIONS)

2. **`data/Marker.kt`** — Room Entity (id, latitude, longitude, type, timestamp)

3. **`data/MarkerDao.kt`** — DAO: getAllMarkers (Flow), insertMarker, deleteMarker

4. **`data/MarkerDatabase.kt`** — Room Database singleton

---

## Step 3: ViewModel

**`viewmodel/MarkersViewModel.kt`**
- StateFlow<UiState> с: markers, currentLocation, isLocationPermissionGranted, placeMarkerMode
- Методы: placeMarker(), deleteMarker(), startPlaceMarker(), cancelPlaceMarker()
- Location: FusedLocationProviderClient для получения геолокации
- Permission handling через callback

---

## Step 4: UI Components

**`ui/components/MarkerPickerBottomSheet.kt`**
- ModalBottomSheet с сеткой типов маркеров (эмодзи + label)
- При выборе — возвращает MarkerType

---

## Step 5: Map Screen

**`ui/screens/MapScreen.kt`**
- GoogleMap с isMyLocationEnabled=true
- Camera position animates to user location
- FAB (Add) → открывает MarkerPickerBottomSheet → выбор типа → tap на карту ставит маркер
- MarkerComposable для отображения маркеров с эмодзи
- Индикатор размещения: "Tap on the map to place 🌲 Christmas Tree"

**Flow размещения маркера:**
1. Tap FAB → открывается picker
2. Выбор типа → pendingMarkerType = selectedType
3. Tap на карту → placeMarker(lat, lng, type)
4. pendingMarkerType = null, кнопка меняется на Close для отмены

---

## Step 6: List Screen

**`ui/screens/ListScreen.kt`**
- Scaffold с TopAppBar "Placed Items"
- GroupBy category → LazyColumn с CategoryHeader + MarkerListItem
- Каждый item: эмодзи, label, координаты, кнопка Delete
- Пустое состояние: "No markers placed yet. Go to the map and add some!"

---

## Step 7: Navigation

**`ui/navigation/AppNavigation.kt`**
- NavHost с двумя экранами: Map ("map"), List ("list")
- BottomNavigation с иконками (Map/List)
- Start destination: Map

---

## Step 8: Update MainActivity

**`MainActivity.kt`** — полностью переписать:
- ChristmasTreesApp composable с Navigation + BottomNavigation
- Shared MarkersViewModel через viewModel()

---

## Порядок реализации

1. ✅ Зависимости (libs.versions.toml + build.gradle.kts + AndroidManifest.xml)
2. ✅ Data layer (MarkerType, Marker, MarkerDao, MarkerDatabase)
3. ✅ ViewModel (MarkersViewModel)
4. ✅ UI Components (MarkerPickerBottomSheet)
5. ✅ Map Screen (MapScreen)
6. ✅ List Screen (ListScreen)
7. ✅ Navigation (AppNavigation)
8. ✅ MainActivity

---

## Verification

1. Приложение запускается, показывает карту с текущей геолокацией
2. FAB → выбор типа → tap на карту → маркер появляется
3. BottomNav переключает между картой и списком
4. Список группирует по категориям (Trees, Snowmen, Decorations)
5. Delete удаляет маркер из списка и с карты
6. Маркеры сохраняются при перезапуске (Room)
7. Запрос location permissions работает корректно
