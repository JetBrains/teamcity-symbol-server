# TeamCity Symbol Server plugin

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![build status](https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TeamCityPluginsByJetBrains_SymbolServer_BuildTrunk)/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_SymbolServer_BuildTrunk&guest=1)

Allows using TeamCity as a symbol and source server.

## Features

* [Symbol server](https://docs.microsoft.com/en-us/windows/desktop/Debug/symbol-servers-and-symbol-stores)
* [Source Server](https://docs.microsoft.com/en-us/windows/desktop/Debug/source-server-and-source-indexing)

## Download

You can [download the plugin](https://plugins.jetbrains.com/plugin/9123-symbol-server) and install it as [an additional TeamCity plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins) for TeamCity 10.0+.

## Requirements

On the build agents should installed:
* [.NET Framework 4.0+](https://www.microsoft.com/net/download/dotnet-framework-runtime)
* [Debugging tools for Windows](https://docs.microsoft.com/en-us/windows-hardware/drivers/debugger/index)

## Reported agent configuration parameters

During initialization this plugin reports the following agent configuration parameters:

* `WinDbg_Path` - is a path to Debugging tools for Windows.

## Usage

TeamCity as a symbol server should be referenced by the following URL `<TEAMCITY_SERVER_URL>/app/symbols`. The exact URL is available on the <b>Administration | Integrations | Symbol Server</b> page.

To enable symbols and sources indexing, add the <b>Symbol Files Indexer</b> [build feature](https://confluence.jetbrains.com/display/TCDL/Adding+Build+Features) to the build configuration. After that all PDB files published as build artifacts of this configuration and all related sources will be indexed. The PDB files packed via TeamCity [artifact publishing rules](https://confluence.jetbrains.com/display/TCDL/Configuring+General+Settings#ConfiguringGeneralSettings-artifactPaths) will be handled correctly.

> For step-by-step instruction, see a related TeamCity [blog post](https://blog.jetbrains.com/teamcity/2015/02/setting-up-teamcity-as-symbol-and-source-server/).

### Authentication/Authorization

The indexed data can be accessed in both modes: public (not requiring authorization) and authorized. The plugin uses standard authentication settings provided by the TeamCity server.

The following permissions are checked:

* `View build runtime parameters and data` - while accessing symbols,
* `View VCS file content` - while accessing sources.

These are the default permissions of [Project Developers](https://confluence.jetbrains.com/display/TCD18/Role+and+Permission).

If you are using the public URL to access symbols, you need to enable the [Guest user login](http://confluence.jetbrains.com/display/TCDL/Guest+User) and [assign](https://confluence.jetbrains.com/display/TCDL/Managing+Users+and+User+Groups#ManagingUsersandUserGroups-assigningRolesAssigningRolestoUsers) the project developer role to the Guest to the Developers group.

When logging into TeamCity as a guest user, the corresponding permissions are checked to grant anonymous access to symbols and sources. If the guest user permissions are insufficient, all enabled HTTP [authentication modules](https://confluence.jetbrains.com/display/TCDL/Authentication+Modules) are applied and the user will be asked for credentials.

The access mode can be configured globally or on a project-level. Also, different access modes can be specified for the symbols and sources.

### Using in docker containers

To use symbol indexing in build agents started from docker agent images you need to [build a derrived image](https://docs.docker.com/engine/reference/commandline/build/) like that:

```dockerfile
FROM jetbrains/teamcity-agent:latest-windowsservercore

# Install Windows SDK
RUN Invoke-WebRequest https://download.microsoft.com/download/5/A/0/5A08CEF4-3EC9-494A-9578-AB687E716C12/windowssdk/winsdksetup.exe?ocid=wdgcx1803-download-installer -OutFile winsdksetup.exe; \
    Start-Process winsdksetup.exe -Wait -ArgumentList /features + /q ; \
    Remove-Item -Force winsdksetup.exe
```

### Overriding source server paths

If the URL of your Teamcity server changes, source indexing will not work for old builds because the old PDB files will still be referencing the old Teamcity URL.  To fix this, you can override the Teamcity source path using the [srcsrv.ini file](https://docs.microsoft.com/en-us/windows-hardware/drivers/debugger/the-srcsrv-ini-file#using_a_different_location_or_file_name).  In the `[variables]` section, set `TEAMCITY_BASE_PATH` to the sources path of the new Teamcity server.  For example:

```ini
[variables]
TEAMCITY_BASE_PATH=https://new_teamcity.example.com/app/sources
```

## Common Problems

### Failed to find Source Server tools home directory
Please ensure that [Debugging tools for Windows](https://docs.microsoft.com/en-us/windows-hardware/drivers/debugger/index) were installed on the build agent.

### Unable to view symbols in IDE

* Ensure that required PDB file was indexed during the build by navigating to the [hidden TeamCity build artifacts](https://confluence.jetbrains.com/display/TCDL/Build+Artifact#BuildArtifact-HiddenArtifacts) and inspect the xml files under `./teamcity/symbols/` directory. In the xml file `sign` attribute contains file signature and `file` atttribute contains file name.
* Navigate in your browser to the page `http://%teamcity%/app/symbols/%pdbFileName%/%fileSignature%/%pdbFileName%` and check that pdb file was downloaded.
* Ensure that IDE use correct credentials to access TeamCity server. Normally VS uses Windows Credentials Manager to get and store credentials. Failed authentication attemps are logged in the `teamcity-auth.log` [server log file](https://confluence.jetbrains.com/display/TCDL/TeamCity+Server+Logs).

## Build

This project uses gradle as a build system. You can easily open it in [IntelliJ IDEA](https://www.jetbrains.com/idea/help/importing-project-from-gradle-model.html) or [Eclipse](http://gradle.org/eclipse/).

## Contributions

We appreciate all kinds of feedback, so please feel free to send a PR or file an issue in the [TeamCity tracker](https://youtrack.jetbrains.com/newIssue?project=TW&summary=Symbol%20Server%3A&c=Subsystem%20plugins%3A%20other&c=tag%20pdb).
