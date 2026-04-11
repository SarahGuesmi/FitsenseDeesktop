package services;

import javafx.collections.ObservableList;
import models.CoachMentalTest;
import models.CoachMentalTestQuestion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists coach mental tests to disk so they survive app restarts.
 * Tests are only removed from storage when {@link CoachMentalTestService#remove} is called.
 */
final class CoachMentalTestPersistence {

    private static final int MAGIC = 0x434D4854; /* CMHT */
    private static final int VERSION = 1;

    private static final String DIR = ".fitsense";
    private static final String FILE_NAME = "coach_mental_tests.v1";

    private CoachMentalTestPersistence() {
    }

    static Path storageFile() {
        return Path.of(System.getProperty("user.home"), DIR, FILE_NAME);
    }

    static void load(ObservableList<CoachMentalTest> tests) {
        Path file = storageFile();
        if (!Files.isRegularFile(file)) {
            return;
        }
        List<CoachMentalTest> loaded = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != MAGIC) {
                return;
            }
            int ver = in.readInt();
            if (ver != VERSION) {
                return;
            }
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                loaded.add(readTest(in));
            }
        } catch (IOException e) {
            try {
                Path bad = file.resolveSibling(FILE_NAME + ".corrupt." + System.currentTimeMillis());
                Files.copy(file, bad, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // ignore
            }
            return;
        }
        tests.clear();
        tests.addAll(loaded);
    }

    static void save(ObservableList<CoachMentalTest> tests) {
        Path file = storageFile();
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            return;
        }
        Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(tests.size());
            for (CoachMentalTest t : tests) {
                writeTest(out, t);
            }
            out.flush();
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // ignore
            }
            return;
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.copy(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    private static CoachMentalTest readTest(DataInputStream in) throws IOException {
        UUID id = readUuid(in);
        String title = in.readUTF();
        LocalDateTime created = readLdt(in);
        LocalDateTime updated = readLdt(in);
        int nq = in.readInt();
        CoachMentalTest t = new CoachMentalTest(id);
        t.setTitle(title);
        t.setCreatedAt(created);
        t.setUpdatedAt(updated);
        for (int i = 0; i < nq; i++) {
            UUID qid = readUuid(in);
            int order = in.readInt();
            String prompt = in.readUTF();
            t.addQuestion(new CoachMentalTestQuestion(qid, order, prompt));
        }
        return t;
    }

    private static void writeTest(DataOutputStream out, CoachMentalTest t) throws IOException {
        writeUuid(out, t.getId());
        out.writeUTF(t.getTitle() == null ? "" : t.getTitle());
        writeLdt(out, t.getCreatedAt());
        writeLdt(out, t.getUpdatedAt());
        out.writeInt(t.getQuestionCount());
        for (CoachMentalTestQuestion q : t.getQuestions()) {
            writeUuid(out, q.getId());
            out.writeInt(q.getOrderIndex());
            out.writeUTF(q.getPrompt() == null ? "" : q.getPrompt());
        }
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static void writeUuid(DataOutputStream out, UUID id) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }

    private static LocalDateTime readLdt(DataInputStream in) throws IOException {
        long ms = in.readLong();
        if (ms < 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
    }

    private static void writeLdt(DataOutputStream out, LocalDateTime t) throws IOException {
        if (t == null) {
            out.writeLong(-1L);
        } else {
            out.writeLong(t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
    }
}
