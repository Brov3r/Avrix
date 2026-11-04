package com.avrix.provider;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Builds synthetic Project Zomboid version classes for resolver tests.
 */
final class VersionJarFixture {

    private static final Handle STRING_CONCAT_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/StringConcatFactory",
            "makeConcatWithConstants",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
            false
    );

    private VersionJarFixture() {
    }

    /**
     * Creates a synthetic {@code projectzomboid.jar} using the provided version components.
     *
     * @param directory directory that receives the archive
     * @param major     major version
     * @param minor     minor version
     * @param suffix    version suffix appended after the minor component
     * @param patch     patch/build component
     * @param revision  Git revision
     * @return path to the generated archive
     * @throws IOException if the archive cannot be written
     */
    static Path createGameArchive(
            Path directory,
            int major,
            int minor,
            String suffix,
            int patch,
            String revision
    ) throws IOException {
        Path archive = directory.resolve("projectzomboid.jar");
        try (OutputStream outputStream = Files.newOutputStream(archive);
             JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            jarOutputStream.putNextEntry(new JarEntry("zombie/core/Core.class"));
            jarOutputStream.write(createCoreClass(major, minor, suffix, patch));
            jarOutputStream.closeEntry();

            jarOutputStream.putNextEntry(new JarEntry("zombie/GitVersion.class"));
            jarOutputStream.write(createGitVersionClass(revision));
            jarOutputStream.closeEntry();
        }
        return archive;
    }

    private static byte[] createCoreClass(int major, int minor, String suffix, int patch) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "zombie/core/Core",
                null,
                "java/lang/Object",
                null
        );

        FieldVisitor buildVersion = writer.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "buildVersion",
                "I",
                null,
                patch
        );
        buildVersion.visitEnd();

        FieldVisitor gameVersion = writer.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "gameVersion",
                "Lzombie/core/GameVersion;",
                null,
                null
        );
        gameVersion.visitEnd();

        MethodVisitor classInitializer = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        classInitializer.visitCode();
        classInitializer.visitTypeInsn(Opcodes.NEW, "zombie/core/GameVersion");
        classInitializer.visitInsn(Opcodes.DUP);
        classInitializer.visitLdcInsn(major);
        classInitializer.visitLdcInsn(minor);
        classInitializer.visitLdcInsn(suffix);
        classInitializer.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "zombie/core/GameVersion",
                "<init>",
                "(IILjava/lang/String;)V",
                false
        );
        classInitializer.visitFieldInsn(
                Opcodes.PUTSTATIC,
                "zombie/core/Core",
                "gameVersion",
                "Lzombie/core/GameVersion;"
        );
        classInitializer.visitInsn(Opcodes.RETURN);
        classInitializer.visitMaxs(0, 0);
        classInitializer.visitEnd();

        MethodVisitor gameAndBuildVersion = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "getGameAndBuildVersion",
                "()Ljava/lang/String;",
                null,
                null
        );
        gameAndBuildVersion.visitCode();
        gameAndBuildVersion.visitInvokeDynamicInsn(
                "makeConcatWithConstants",
                "()Ljava/lang/String;",
                STRING_CONCAT_BOOTSTRAP,
                "\u0001." + patch
        );
        gameAndBuildVersion.visitInsn(Opcodes.ARETURN);
        gameAndBuildVersion.visitMaxs(0, 0);
        gameAndBuildVersion.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] createGitVersionClass(String revision) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "zombie/GitVersion",
                null,
                "java/lang/Object",
                null
        );

        FieldVisitor revisionField = writer.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "REVISION",
                "Ljava/lang/String;",
                null,
                revision
        );
        revisionField.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }
}
