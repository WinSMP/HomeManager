rootProject.name = "HomeManager"

extra["minecraftBase"] = "1.21"
extra["minecraftPatch"] = "${extra["minecraftBase"]}.4"
extra["projectDescription"] = "A Minecraft plugin to manage player homes with location saving and teleportation."

gradle.beforeProject {
    if (this == rootProject) {
        extra["minecraftBase"] = settings.extra["minecraftBase"] as String
        extra["minecraftPatch"] = settings.extra["minecraftPatch"] as String
        extra["projectDescription"] = settings.extra["projectDescription"] as String
    }
}
