// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
// Configuration pour tous les projets
allprojects {
    // Si tu utilises une ancienne version d'Android Studio, tu peux avoir besoin de ceci :
    // repositories {
    //     google()
    //     mavenCentral()
    // }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}