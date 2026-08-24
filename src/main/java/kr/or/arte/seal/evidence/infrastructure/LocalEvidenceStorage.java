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
