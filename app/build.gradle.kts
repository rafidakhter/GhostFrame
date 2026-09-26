import java.io.RandomAccessFile
import org.gradle.work.DisableCachingByDefault

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ghostframe"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.ghostframe"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation("io.coil-kt.coil3:coil:3.3.0")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

/** Runs at execution time, including when Gradle reuses the configuration cache. */
@DisableCachingByDefault(because = "Each debug build needs a new local identifier")
abstract class GenerateDebugBuildLabel : DefaultTask() {
    @get:Internal
    abstract val counterFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generate() {
        val counter = counterFile.get().asFile
        counter.parentFile.mkdirs()
        val number = RandomAccessFile(counter, "rw").use { file ->
            file.channel.lock().use {
                val previous = if (file.length() == 0L) 0L else
                    file.readLine().trim().toLongOrNull()
                        ?: error("Invalid debug build counter in $counter")
                val next = Math.addExact(previous, 1L)
                file.seek(0)
                file.setLength(0)
                file.writeBytes("$next\n")
                next
            }
        }
        val values = outputDirectory.dir("values").get().asFile
        values.mkdirs()
        values.resolve("build_label.xml").writeText(
            """<resources><string name="build_identifier" translatable="false">Debug build $number</string></resources>"""
        )
        logger.lifecycle("GhostFrame: Debug build $number")
    }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val label = tasks.register<GenerateDebugBuildLabel>("generateDebugBuildLabel") {
            counterFile.set(rootProject.layout.projectDirectory.file(".debug-build-number"))
        }
        variant.sources.res?.addGeneratedSourceDirectory(label, GenerateDebugBuildLabel::outputDirectory)
    }
}
