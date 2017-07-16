//     Project: SBT NPM
//      Module:
// Description:
package de.surfice.sbtnpm

import de.surfice.sbtnpm.utils.{ExternalCommand, FileWithLastrun}
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt._
import Keys._
import Cache._
import sbt.complete.Parser

object NpmPlugin extends AutoPlugin {
  type NpmDependency = (String,String)

  override lazy val requires = ScalaJSPlugin

  // Exported keys
  /**
   * @groupname tasks Tasks
   * @groupname settings Settings
   */
  object autoImport {
    /**
     * Defines the directory in which the npm `node_modules` resides.
     *
     * Defaults to `baseDirectory.value`.
     *
     * @group settings
     */
    val npmTargetDir: SettingKey[File] =
      settingKey[File]("Root directory of the npm project")

    val npmNodeModulesDir: SettingKey[File] =
      settingKey("Path to the node_modules dir")

    /**
      * List of the NPM packages (name and version) your application depends on.
      * You can use [semver](https://docs.npmjs.com/misc/semver) versions:
      *
      * {{{
      *   npmDependencies += "uuid" -> "~3.0.0"
      * }}}
      *
      * @group settings
      */
    val npmDependencies: SettingKey[Seq[NpmDependency]] =
      settingKey[Seq[NpmDependency]]("NPM dependencies (libraries that your program uses)")

    /** @group settings */
    val npmDevDependencies: SettingKey[Seq[NpmDependency]] =
      settingKey[Seq[NpmDependency]]("NPM dev dependencies (libraries that the build uses)")

    /**
     * Defines the path to the package.json file generated by the [[npmWritePackageJson]] task.
     *
     * Default: `npmTargetDirectory.value / "package.json"`
     *
     * @group settings
     */
    val npmPackageJsonFile: SettingKey[File] =
      settingKey[File]("Full path to the npm package.json file")

    val npmPackageJson: SettingKey[PackageJson] =
      settingKey[PackageJson]("Defines the contents of the npm package.json file")

    val npmWritePackageJson: TaskKey[FileWithLastrun] =
      taskKey[FileWithLastrun]("Create the npm package.json file.")

    /**
     *
     * @group tasks
     */
    val npmInstall: TaskKey[Long] =
      taskKey[Long]("Install npm dependencies")

    val npmRunScript: InputKey[Unit] =
      inputKey[Unit]("Run the specified npm script")

    val npmMain: SettingKey[Option[String]] =
      settingKey[Option[String]]("package.json 'main' property")

    val npmScripts: SettingKey[Seq[(String,String)]] =
      settingKey[Seq[(String,String)]]("npm scripts")

    val npmCmd: SettingKey[ExternalCommand] =
      settingKey[ExternalCommand]("npm command")

  }


  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    npmCmd := ExternalCommand("npm"),

    npmTargetDir := baseDirectory.value,

    npmNodeModulesDir := npmTargetDir.value / "node_modules",

    npmPackageJsonFile := npmTargetDir.value / "package.json",

    npmDependencies := Nil,

    npmDevDependencies := Nil,

    npmMain := None,

    npmScripts := Nil,

    npmPackageJson := PackageJson(
      path = npmPackageJsonFile.value,
      name = name.value,
      version = version.value,
      description = description.value,
      dependencies = npmDependencies.value,
      devDependencies = npmDevDependencies.value,
      main = npmMain.value,
      scripts = npmScripts.value
    ),

    npmWritePackageJson := {
      val file = npmPackageJsonFile.value
      val lastrun = npmWritePackageJson.previous
      if(lastrun.isEmpty || lastrun.get.needsUpdateComparedToConfig(baseDirectory.value)) {
        npmPackageJson.value.writeFile()(streams.value.log)
        FileWithLastrun(file)
      }
      else
        lastrun.get
    },

    npmInstall := {
      val file = npmWritePackageJson.value
      val lastrun = npmInstall.previous
      if(lastrun.isEmpty || file.lastrun>lastrun.get) {
        ExternalCommand.npm.install(npmTargetDir.value.getCanonicalFile,npmNodeModulesDir.value.getCanonicalFile,streams.value.log)
        new java.util.Date().getTime
      }
      else
        lastrun.get
    },

    npmRunScript := {
      import complete.DefaultParsers._

      npmInstall.value
      val script = spaceDelimited("<arg>").parsed.head
      ExternalCommand.npm.start("run-script",script)(streams.value.log,waitAndKillOnInput = true)
    }
  )
}

