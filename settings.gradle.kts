rootProject.name = "parcellockers"

include("parcellockers-api")
include("parcellockers-plugin")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
