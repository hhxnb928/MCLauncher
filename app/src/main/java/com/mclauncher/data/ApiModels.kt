package com.mclauncher.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== Mojang Version Manifest =====
@Serializable
data class MojangVersionManifest(
    val latest: MojangLatest,
    val versions: List<MojangVersion>
)

@Serializable
data class MojangLatest(
    val release: String,
    val snapshot: String
)

@Serializable
data class MojangVersion(
    val id: String,
    val type: String,      // release / snapshot / old_beta / old_alpha
    val url: String,
    val time: String,
    @SerialName("releaseTime") val releaseTime: String,
    val sha1: String? = null,
    val compliantLevel: Int? = null
)

// ===== 本地安装版本 =====
@Serializable
data class LocalVersion(
    val id: String,
    val type: String,
    val installed: Boolean = false,
    val installedAt: Long = 0,
    val loader: LoaderType = LoaderType.VANILLA,
    val loaderVersion: String = ""
)

enum class LoaderType {
    VANILLA, FABRIC, FORGE, NEOFORGE, QUILT
}

// ===== Modrinth API =====
@Serializable
data class ModrinthSearchResponse(
    val hits: List<ModrinthMod>,
    val offset: Int = 0,
    val limit: Int = 20,
    val totalHits: Int = 0
)

@Serializable
data class ModrinthMod(
    val slug: String,
    val title: String,
    val description: String,
    val author: String? = null,
    val authors: List<ModrinthAuthor> = emptyList(),
    val categories: List<String> = emptyList(),
    val downloads: Long = 0,
    val follows: Long = 0,
    val iconUrl: String? = null,
    val projectType: String = "mod",
    val versions: List<String> = emptyList(),
    val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList()
)

@Serializable
data class ModrinthAuthor(
    val name: String,
    val url: String? = null
)

@Serializable
data class ModrinthVersion(
    val id: String,
    val name: String,
    val versionNumber: String,
    val gameVersions: List<String>,
    val loaders: List<String>,
    val files: List<ModrinthFile>,
    val dependencies: List<ModrinthDependency> = emptyList()
)

@Serializable
data class ModrinthFile(
    val url: String,
    val filename: String,
    val primary: Boolean = false,
    val size: Long = 0
)

@Serializable
data class ModrinthDependency(
    val projectId: String? = null,
    val versionId: String? = null,
    val dependencyType: String = "required"
)

// ===== 下载状态 =====
data class DownloadTask(
    val id: String,
    val name: String,
    val url: String,
    val outputPath: String,
    var progress: Float = 0f,
    var downloadedBytes: Long = 0,
    var totalBytes: Long = 0,
    var status: Status = Status.PENDING,
    var speed: Float = 0f,  // bytes/sec
    var description: String = ""
) {
    enum class Status { PENDING, RUNNING, PAUSED, COMPLETED, ERROR }
}
