import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.android.application)
}

kotlin {
	applyDefaultHierarchyTemplate()
	jvm("desktop")
	androidTarget {
		compilations.all {
			kotlinOptions {
				jvmTarget = libs.versions.jvm.get()
			}
		}
	}
	wasmJs {
		moduleName = "sampleApp"
		browser {
			commonWebpackConfig {
				outputFileName = "sampleApp.js"
			}
		}
		binaries.executable()
	}

	sourceSets {

		val commonMain by getting {
			dependencies {
				implementation(project(":ComposeTextEditor"))
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.materialIconsExtended)
				implementation(compose.ui)
				implementation(compose.components.resources)
				implementation(compose.components.uiToolingPreview)
			}
		}

		val desktopMain by getting {
			dependencies {
				implementation(libs.kotlinx.coroutines.swing)

				implementation(libs.androidx.lifecycle.viewmodel)
				implementation(libs.androidx.lifecycle.runtime.compose)

				implementation(compose.desktop.currentOs)
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.materialIconsExtended)
				implementation(compose.ui)
				implementation(compose.components.resources)
				implementation(compose.components.uiToolingPreview)
			}
		}

		val desktopTest by getting {
			dependencies {
				implementation(libs.jetbrains.kotlin.test)
				implementation(libs.jetbrains.kotlin.test.junit)
				implementation(libs.mockk)
				implementation(libs.kotlinx.coroutines.test)
				implementation(libs.kotlinx.coroutines.test.jvm)
			}
		}

		val androidMain by getting {
			dependencies {
				implementation(libs.activity.ktx)
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.ui)
				implementation(compose.components.resources)
			}
		}
	}
}

compose.android {

}

compose.desktop {
	application {
		mainClass = "MainKt"

		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "com.darkrockstudios.texteditor"
			packageVersion = "1.0.0"
		}
	}
}

compose.experimental {
	web.application {}
}

android {
	namespace = "com.darkrockstudios.texteditor.sample"
	compileSdk = 35
	defaultConfig {
		applicationId = "com.darkrockstudios.texteditor.sample"
		minSdk = 26
		targetSdk = 35
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	buildFeatures {
		compose = true
	}
	compileOptions {
		sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
		targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
	}

	buildTypes {
		debug {
		}

		release {
		}
	}
}
dependencies {
	implementation(libs.lifecycle.runtime.ktx)
	implementation(libs.activity.compose)
	implementation(platform(libs.compose.bom))
	implementation(libs.ui)
	implementation(libs.ui.graphics)
	implementation(libs.ui.tooling.preview)
	implementation(libs.material3)
	implementation(libs.activity.ktx)
	androidTestImplementation(platform(libs.compose.bom))
	androidTestImplementation(libs.ui.test.junit4)
	debugImplementation(libs.ui.tooling)
	debugImplementation(libs.ui.test.manifest)
}
