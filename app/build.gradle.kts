plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.csia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.company.csia"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //fix for EdgeToEdge
    implementation("androidx.activity:activity:1.8.0")

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))

    // Add specific Firebase services here
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.database)

    //Add Google Calendar API
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.api-client:google-api-client-android:1.23.0")
    implementation("com.google.api-client:google-api-client-gson:1.23.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev305-1.25.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev305-1.25.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}