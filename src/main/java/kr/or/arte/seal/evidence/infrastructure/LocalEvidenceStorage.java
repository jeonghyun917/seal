package kr.or.arte.seal.evidence.infrastructure;

import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public final class LocalEvidenceStorage {
    private final Path root;

    public LocalEvidenceStorage(Path root) {
        try {
            Files.createDirectories(root);
            this.root = root.toRealPath();
        } catch (Exception exception) {
            throw new IllegalStateException("증적 저장소를 준비할 수 없습니다.", exception);
        }
    }

    public Path write(UUID organizationId, UUID requestId, UUID packageId, Map<String, byte[]> files) {
        Path parent = safeResolve(organizationId.toString(), requestId.toString());
        Path target = parent.resolve(packageId.toString()).normalize();
        Path temporary = parent.resolve(".tmp-" + UUID.randomUUID()).normalize();
        if (Files.exists(target)) return requireIdentical(target, files);
        try {
            Files.createDirectories(temporary);
            for (var entry : files.entrySet()) {
                if (!entry.getKey().matches("[A-Za-z0-9._-]{1,100}") || entry.getKey().contains("..")) {
                    throw new IllegalArgumentException("안전하지 않은 증적 파일명입니다.");
                }
                Path file = temporary.resolve(entry.getKey()).normalize();
                ensureInside(file);
                Files.write(file, entry.getValue(), StandardOpenOption.CREATE_NEW);
                try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
                    channel.force(true);
                }
            }
            try {
                return Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                return Files.move(temporary, target);
            }
        } catch (RuntimeException exception) {
            deleteQuietly(temporary);
            throw exception;
        } catch (Exception exception) {
            deleteQuietly(temporary);
            throw new IllegalStateException("증적 파일을 저장할 수 없습니다.", exception);
        }
    }

    public byte[] read(String relativePath) {
        try { return Files.readAllBytes(resolveRelative(relativePath)); }
        catch (Exception exception) { throw new IllegalStateException("증적 파일을 읽을 수 없습니다.", exception); }
    }

    public Path resolveRelative(String relativePath) {
        if (relativePath == null || relativePath.contains("..") || Path.of(relativePath).isAbsolute())
            throw new IllegalArgumentException("안전하지 않은 증적 경로입니다.");
        Path resolved=root.resolve(relativePath.replace('/',java.io.File.separatorChar)).normalize();
        ensureInside(resolved); return resolved;
    }

    public int deleteDirectory(String relativePath) {
        Path target=resolveRelative(relativePath);if(target.equals(root))throw new IllegalArgumentException("증적 루트는 삭제할 수 없습니다.");
        if(!Files.exists(target))return 0;try{var paths=Files.walk(target).sorted(Comparator.reverseOrder()).toList();int files=(int)paths.stream().filter(Files::isRegularFile).count();for(Path path:paths)Files.deleteIfExists(path);return files;}catch(Exception e){throw new IllegalStateException("증적 파일을 삭제할 수 없습니다.",e);}
    }

    public QuarantinedEvidence quarantine(UUID jobId, String relativePath) {
        if (jobId == null) throw new IllegalArgumentException("파기 작업 ID가 필요합니다.");
        Path source = resolveRelative(relativePath);
        String stableName = UUID.nameUUIDFromBytes(relativePath.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        String quarantinePath = ".quarantine/" + jobId + "/" + stableName;
        Path target = resolveRelative(quarantinePath);
        try {
            if (Files.exists(target)) {
                return new QuarantinedEvidence(relativePath, quarantinePath, countFiles(target));
            }
            if (!Files.exists(source)) {
                return new QuarantinedEvidence(relativePath, quarantinePath, 0);
            }
            Files.createDirectories(target.getParent());
            try {
                Files.move(source, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(source, target);
            }
            return new QuarantinedEvidence(relativePath, quarantinePath, countFiles(target));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("증적 파일을 격리할 수 없습니다.", exception);
        }
    }

    public int purgeQuarantine(String quarantinePath) {
        if (quarantinePath == null || !quarantinePath.startsWith(".quarantine/")) {
            throw new IllegalArgumentException("격리 경로가 올바르지 않습니다.");
        }
        return deleteDirectory(quarantinePath);
    }

    public int purgeJobQuarantine(UUID jobId) {
        if (jobId == null) throw new IllegalArgumentException("파기 작업 ID가 필요합니다.");
        return purgeQuarantine(".quarantine/" + jobId);
    }

    private static Path requireIdentical(Path target, Map<String, byte[]> expected) {
        try (var entries = Files.list(target)) {
            var existing = entries.toList();
            boolean identical = existing.size() == expected.size()
                    && existing.stream().allMatch(Files::isRegularFile);
            if (identical) {
                for (var entry : expected.entrySet()) {
                    Path file = target.resolve(entry.getKey()).normalize();
                    if (!file.getParent().equals(target)
                            || !Files.exists(file)
                            || !java.security.MessageDigest.isEqual(Files.readAllBytes(file), entry.getValue())) {
                        identical = false;
                        break;
                    }
                }
            }
            if (!identical) throw new IllegalStateException("같은 증적 패키지 경로에 다른 내용이 이미 있습니다.");
            return target;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("기존 증적 패키지를 확인할 수 없습니다.", exception);
        }
    }
    private static int countFiles(Path target) {
        if (!Files.exists(target)) return 0;
        try (var paths = Files.walk(target)) {
            return (int) paths.filter(Files::isRegularFile).count();
        } catch (Exception exception) {
            throw new IllegalStateException("격리 파일 수를 확인할 수 없습니다.", exception);
        }
    }

    private Path safeResolve(String... parts) {
        Path path = root;
        for (String part : parts) path = path.resolve(part);
        path = path.normalize();
        ensureInside(path);
        try { Files.createDirectories(path); } catch (Exception e) { throw new IllegalStateException(e); }
        return path;
    }

    private void ensureInside(Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(root)) {
            throw new IllegalArgumentException("증적 경로가 설정 루트를 벗어났습니다.");
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            if (Files.exists(path)) Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) { }
            });
        } catch (Exception ignored) { }
    }
}
