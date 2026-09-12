plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.ksp) apply false
    // Applied, not `apply false`: the scanner runs from the root and walks the
    // build from there, and this is also the line that creates the `sonar`
    // extension the app module configures.
    alias(libs.plugins.sonarqube)
}

/**
 * SonarQube Cloud. The keys are the ones SonarCloud generated when the
 * repository was imported, and they are what the README coverage badges are
 * read against - changing either here without changing the badges leaves the
 * badges pointing at a project that no longer reports.
 *
 * The token is not here and never will be: CI passes it as `SONAR_TOKEN`.
 */
sonar {
    properties {
        property("sonar.projectKey", "drehtuer_ShotgunApp")
        property("sonar.organization", "drehtuer")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "Shotgun!")
    }
}
