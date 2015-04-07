lazy val commonSettings = Seq(
  organization := "com.nativelibs4java",
  version := "0.4-SNAPSHOT",
  scalaVersion := "2.11.6",
  homepage := Some(url("https://github.com/nativelibs4java/scalaxy-reified")),
  pomExtra := (
    <scm>
      <url>git@github.com:nativelibs4java/scalaxy-reified.git</url>
      <connection>scm:git:git@github.com:nativelibs4java/scalaxy-reified.git</connection>
    </scm>
    <developers>
      <developer>
        <id>ochafik</id>
        <name>Olivier Chafik</name>
        <url>http://ochafik.com/</url>
      </developer>
    </developers>
  ),
  licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")),
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  publishTo <<= version(v => Some(
    if (v.trim.endsWith("-SNAPSHOT"))
      "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")),
  credentials ++= (for {
    username <- Option(System.getenv("SONATYPE_USERNAME"));
    password <- Option(System.getenv("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager",
                      "oss.sonatype.org", username, password)
  ).toSeq,
  shellPrompt := { s => Project.extract(s).currentProject.id + "> " },
  resolvers += Resolver.defaultLocal,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
  libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
  libraryDependencies ++= Seq(
    "junit" % "junit" % "4.12" % "test",
    "com.novocode" % "junit-interface" % "0.11" % "test"
  ),
  testOptions in Global += Tests.Argument(TestFrameworks.JUnit, "-v"),
  fork in Test := true,
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-deprecation", "-feature", "-unchecked",
    "-optimise", "-Yclosure-elim", "-Yinline",
    "-Xlog-free-types"
  )
)

lazy val reifiedBase = (project in file("Base")).
  settings(commonSettings ++ Seq(
    name := "scalaxy-reified-base",
    libraryDependencies <+= (organization, version) {
      case (organization, version) =>
        organization %% "scalaxy-generic" % version
    }
  ): _*)

lazy val reified = (project in file(".")).
  settings(name := "scalaxy-reified").
  settings(commonSettings: _*).
  dependsOn(reifiedBase).
  aggregate(reifiedBase)

lazy val reifiedDoc = (project in file("Doc")).
  settings(commonSettings: _*).
  settings(site.settings ++ site.includeScaladoc() ++ ghpages.settings: _*).
  settings(
    publish := { },
    (skip in compile) := true,
    git.remoteRepo := "git@github.com:nativelibs4java/scalaxy-reified.git",
    scalacOptions in (Compile, doc) <++= (name, baseDirectory, description, version, sourceDirectory) map {
      case (name, base, description, version, sourceDirectory) =>
        Opts.doc.title(name + ": " + description) ++
        Opts.doc.version(version) ++
        //Seq("-doc-source-url", "https://github.com/nativelibs4java/scalaxy-reified/blob/master/Base/src/main/scala") ++
        Seq("-doc-root-content", (sourceDirectory / "main" / "rootdoc.txt").getAbsolutePath)
    },
    unmanagedSourceDirectories in Compile <<= (
      (Seq(reified, reifiedBase) map (unmanagedSourceDirectories in _ in Compile)).join.apply {
        (s) => s.flatten.toSeq
      }
    )
  ).
  dependsOn(reified, reifiedBase)
