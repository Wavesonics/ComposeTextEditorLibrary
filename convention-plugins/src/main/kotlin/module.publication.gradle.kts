plugins {
	`maven-publish`
	signing
}

publishing {
	// Configure all publications
	publications.withType<MavenPublication> {
		// Stub javadoc.jar artifact
		artifact(tasks.register("${name}JavadocJar", Jar::class) {
			archiveClassifier.set("javadoc")
			archiveAppendix.set(this@withType.name)
		})

		artifactId = artifactId.lowercase()

		// Provide artifacts information required by Maven Central
		pom {
			name.set("Compose Text Editor")
			description.set("A Kotlin Multiplatform Text Editor.")
			url.set("https://github.com/Wavesonics/ComposeTextEditorLibrary")

			licenses {
				license {
					name.set("MIT")
					url.set("https://opensource.org/licenses/MIT")
				}
			}
			issueManagement {
				system.set("Github")
				url.set("https://github.com/Wavesonics/ComposeTextEditorLibrary/issues")
			}
			scm {
				connection.set("scm:git:git://github.com/Wavesonics/ComposeTextEditorLibrary.git")
				developerConnection.set("scm:git:ssh://github.com/Wavesonics/ComposeTextEditorLibrary.git")
				url.set("https://github.com/Wavesonics/ComposeTextEditorLibrary")
			}
			developers {
				developer {
					name.set("Adam Brown")
					id.set("Wavesonics")
					email.set("adamwbrown@gmail.com")
				}
			}
		}
	}
}


signing {
	val signingKey: String? = System.getenv("SIGNING_KEY")
	val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
	if (signingKey != null && signingPassword != null) {
		useInMemoryPgpKeys(null, signingKey, signingPassword)
		sign(publishing.publications)
	} else {
		println("No signing credentials provided. Skipping Signing.")
	}
}

// TODO: remove after https://youtrack.jetbrains.com/issue/KT-46466 is fixed
project.tasks.withType(AbstractPublishToMaven::class.java).configureEach {
	dependsOn(project.tasks.withType(Sign::class.java))
}