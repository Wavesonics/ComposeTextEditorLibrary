import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
}

kotlin {
	jvm("desktop")

	sourceSets {
		val desktopMain by getting {
			dependencies {
				implementation(project(":ComposeTextEditor"))

				implementation(compose.desktop.currentOs)
				implementation(libs.kotlinx.coroutines.swing)

				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.materialIconsExtended)
				implementation(compose.ui)
				implementation(compose.components.resources)
				implementation(compose.components.uiToolingPreview)
				implementation(libs.androidx.lifecycle.viewmodel)
				implementation(libs.androidx.lifecycle.runtime.compose)
			}
		}

		val desktopTest by getting {
			dependencies {
				implementation("org.jetbrains.kotlin:kotlin-test")
				implementation("org.jetbrains.kotlin:kotlin-test-junit")
				implementation("io.mockk:mockk:1.13.8")
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.7.3")
			}
		}
	}
}


compose.desktop {
	application {
		mainClass = "MainKt"

		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "org.darkrockstudios.texteditor"
			packageVersion = "1.0.0"
		}
	}
}
