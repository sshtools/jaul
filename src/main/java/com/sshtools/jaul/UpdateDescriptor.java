package com.sshtools.jaul;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.install4j.api.Util;

public class UpdateDescriptor {

	public enum MediaType {
		INSTALLER, RPM, DEB, ARCHIVE;

		public String pattern() {
			switch (this) {
			case INSTALLER:
				return ".*\\.sh|.*\\.exe|.*\\.msi|.*\\.dmg";
			case RPM:
				return ".*\\.rpm";
			case DEB:
				return ".*\\.deb";
			case ARCHIVE:
				return ".*\\.zip|.*\\.tgz|.*\\.tar\\.gz";
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	public enum MediaOS {
		LINUX, WINDOWS, MACOS, UNIX;

		public String pattern() {
			switch (this) {
			case LINUX:
				return ".*-linux-.*\\.sh|.*-linux-.*\\.zip|.*-linux-.*\\.tgz|.*-windows-.*\\.tar\\.gz|.*\\.rpm|.*\\.deb";
			case WINDOWS:
				return ".*\\.msi|.*\\.exe|.*-windows-.*\\.zip|.*-windows-.*\\.tgz|.*-windows-.*\\.tar\\.gz";
			case MACOS:
				return ".*\\.dmg|.*-mac-.*\\.zip|.*-macos-.*\\.tgz|.*-macos-.*\\.tar\\.gz|.*-mac-.*\\.zip|.*-macos-.*\\.tgz|.*-macos-.*\\.tar\\.gz";
			default:
				return ".*\\.sh|.*-unix-.*\\.zip|.*-unix-.*\\.tgz|.*-unix-.*\\.tar\\.gz";
			}
		}

		public static MediaOS get() {
			if (Util.isWindows()) {
				return MediaOS.WINDOWS;
			} else if (Util.isMacOS()) {
				return MediaOS.MACOS;
			} else if (Util.isLinux()) {
				return MediaOS.LINUX;
			} else {
				return MediaOS.UNIX;
			}
		}
	}

	public enum MediaArch {
		X86, X86_64, ARM32, AARCH64, XPLATFORM;

		public String pattern() {
			switch (this) {
			case X86:
				return ".*-x86-.*";
			case X86_64:
				return ".*-(x64|amd64|x8664|x86_64|x86-64)-.*";
			case AARCH64:
				return ".*-(aarch64|arm64)-.*";
			case ARM32:
				return ".*-(arm.*)-.*";
			default:
				return ".*";
			}
		}

		public static MediaArch get() {
			var arch = System.getProperty("os.arch", "unknown");
			if (Util.is64BitWindows()) {
				return MediaArch.X86_64;
			} else if (arch.equals("x86_64") || arch.equals("amd64") || arch.equals("ia64")) {
				return MediaArch.X86_64;
			} else if (arch.equals("aarch64")) {
				return MediaArch.AARCH64;
			} else if (arch.equals("aarch32") || arch.equals("arm")) {
				return MediaArch.ARM32;
			} else if (arch.equals("x86")) {
				return MediaArch.X86;
			} else {
				return MediaArch.XPLATFORM;
			}
		}
	}

	public final static class Media {
		private final MediaKey key;
		private final URL url;
		private final long fileSize;
		private final String name;
		private final String md5Sum;
		private final String sha256Sum;
		private final String version;

		Media(MediaKey key, String name, URL url, long fileSize, String md5Sum, String sha256Sum, String version) {
			super();
			this.version = version;
			this.name = name;
			this.key = key;
			this.url = url;
			this.fileSize = fileSize;
			this.md5Sum = md5Sum;
			this.sha256Sum = sha256Sum;
		}

		public final String version() {
			return version;
		}

		public final MediaKey key() {
			return key;
		}

		public final URL url() {
			return url;
		}

		public final long fileSize() {
			return fileSize;
		}

		public final String name() {
			return name;
		}

		public final String md5Sum() {
			return md5Sum;
		}

		public final String sha256Sum() {
			return sha256Sum;
		}

		@Override
		public String toString() {
			return "Media [key=" + key + ", url=" + url + ", fileSize=" + fileSize + ", name=" + name + ", md5Sum="
					+ md5Sum + ", sha256Sum=" + sha256Sum + ", version=" + version + "]";
		}
	}

	public final static class MediaKey {
		private final MediaOS os;
		private final MediaArch arch;
		private final MediaType type;
		private final String variant;

		public MediaKey(MediaOS os, MediaArch arch, MediaType type, String variant) {
			super();
			this.os = os;
			this.variant = variant;
			this.arch = arch;
			this.type = type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((arch == null) ? 0 : arch.hashCode());
			result = prime * result + ((os == null) ? 0 : os.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((variant == null) ? 0 : variant.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MediaKey other = (MediaKey) obj;
			if (arch != other.arch)
				return false;
			if (os != other.os)
				return false;
			if (type != other.type)
				return false;
			if (variant == null) {
				if (other.variant != null)
					return false;
			} else if (!variant.equals(other.variant))
				return false;
			return true;
		}

		public String variant() {
			return variant;
		}

		public static MediaKey get() {
			return new MediaKey(MediaOS.get(), MediaArch.get(), MediaType.INSTALLER, null);
		}

		public MediaArch arch() {
			return arch;
		}

		public MediaOS os() {
			return os;
		}

		@Override
		public String toString() {
			return "MediaKey [os=" + os + ", arch=" + arch + ", type=" + type + ", variant=" + variant + "]";
		}

		public MediaType type() {
			return type;
		}

	}

	private final Map<MediaKey, Media> mediaUrls = new TreeMap<>((o1, o2) -> {
		var cmp = o1.os().compareTo(o2.os());
		if (cmp == 0) {
			cmp = o1.arch().compareTo(o2.arch());
			if (cmp == 0) {
				cmp = o1.type().compareTo(o2.type());
				if (cmp == 0) {
					return Objects.compare(o1.variant(), o2.variant(), (v1, v2) -> {
						if (v1 == null) {
							if (v2 == null)
								return 0;
							else
								return -1;
						} else if (v2 == null) {
							return 1;
						} else {
							return v1.compareTo(v2);
						}
					});
				} else {
					return cmp;
				}
			} else {
				return cmp;
			}
		} else {
			return cmp;
		}
	});

	public static UpdateDescriptor get(URI uri) throws IOException {
		try {
			var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
			var request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofMinutes(2)).GET().build();

			var response = client.send(request, BodyHandlers.ofInputStream());

			if (response.statusCode() == 200) {
				return new UpdateDescriptor(response.body());

			} else {
				throw new IOException("Unexpected response code for " + uri + ". " + response.statusCode());
			}
		} catch (InterruptedException e) {
			throw new IOException("Failed to load remote descriptor.", e);
		}
	}

	public UpdateDescriptor(InputStream in) throws IOException {
		try {
			var docBuilderFactory = DocumentBuilderFactory.newInstance();
			var docBuilder = docBuilderFactory.newDocumentBuilder();
			var doc = docBuilder.parse(in);
			var ud = doc.getDocumentElement();
			var mediaBaseUrl = ud.getAttributes().getNamedItem("baseUrl").getTextContent();
			var entries = doc.getDocumentElement().getElementsByTagName("entry");

			for (int i = 0; i < entries.getLength(); i++) {
				var el = entries.item(i);

				var fileSize = Long.parseLong(el.getAttributes().getNamedItem("fileSize").getTextContent());
				var md5Sum = getAttrVal(el, "md5Sum");
				var version = getAttrVal(el, "newVersion");
				var sha256Sum = getAttrVal(el, "sha256Sum");
				var fileName = el.getAttributes().getNamedItem("fileName").getTextContent();
				var bundledJre = el.getAttributes().getNamedItem("bundledJre").getTextContent();

				var idx = fileName.lastIndexOf('.');
				var variant = fileName.substring(idx + 1);
				if (variant.equals("gz") || variant.equals("bz")) {
					idx = fileName.lastIndexOf('.', idx - 1);
					if (idx != -1)
						variant = fileName.substring(idx + 1);
				}

				MediaType mediaType = null;
				for (var type : MediaType.values()) {
					if (fileName.matches(type.pattern())) {
						mediaType = type;
						break;
					}
				}
				if (mediaType == null) {
					Logging.warn("Skipping {0} in descriptor, it doesn't match any media type.", fileName);
					continue;
				}

				MediaOS mediaOs = null;
				for (var os : MediaOS.values()) {
					if (fileName.matches(os.pattern())) {
						mediaOs = os;
						break;
					}
				}
				if (mediaOs == null) {
					Logging.warn("Skipping {0} in descriptor, it doesn't match any OS.", fileName);
					continue;
				}

				var mediaArch = MediaArch.XPLATFORM;
				for (var arch : MediaArch.values()) {
					if (fileName.matches(arch.pattern())) {
						if (bundledJre.matches("linux-.*") && mediaOs == MediaOS.UNIX) {
							mediaOs = MediaOS.LINUX;
						}
						mediaArch = arch;
						break;
					}
				}

				var mediaKey = new MediaKey(mediaOs, mediaArch, mediaType, variant);
				var media = new Media(mediaKey, fileName, new URL(new URL(mediaBaseUrl), fileName), fileSize, md5Sum,
						sha256Sum, version);
				mediaUrls.put(mediaKey, media);

			}

		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException("Failed to load remote descriptor.", e);
		}
	}

	public final Map<MediaKey, Media> getMediaUrls() {
		return mediaUrls;
	}
	
	public Optional<Media> find(MediaKey key) {
		var media = mediaUrls.get(key);
		if(media == null) {
			if(key.variant == null) {
				for(var en : mediaUrls.entrySet()) {
					if(Objects.equals(en.getKey().os, key.os) &&
							Objects.equals(en.getKey().arch, key.arch) &&
							Objects.equals(en.getKey().type, key.type)) {
						return Optional.of(en.getValue());
					}
				}
			}
			return Optional.empty();
		}
		else {
			return Optional.of(media);
		}
		
	}

	public final Optional<Media> getMedia() {
		var key = MediaKey.get();
		var media = find(key);
		if (media.isEmpty() && key.arch() != MediaArch.XPLATFORM) {
			key = new MediaKey(key.os(), MediaArch.XPLATFORM, key.type(), null);
			media = find(key);
		}
		return media;
	}

	private String getAttrVal(Node el, String name) {
		var attr = el.getAttributes().getNamedItem(name);
		return attr == null ? null : attr.getTextContent();
	}

}
