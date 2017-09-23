package ru.ra;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ru.ra.Environment.IDisposable;
import ru.ra.Environment.UnpublishedException;

public class EnvironmentTest {
    private static class Sys implements IDisposable {
        private boolean disposed;

        public boolean isDisposed() {
            return this.disposed;
        }

        public void setDisposed(boolean disposed) {
            this.disposed = disposed;
        }

        @Override
        public void dispose() {
            setDisposed(true);
        }
    }

    @Test
    public void testPublishedNoThrow() throws UnpublishedException {
        Sys sys = new Sys();
        Environment.publish(Sys.class, sys);
        Environment.getPublished(Sys.class);
    }

    @Test(expected = UnpublishedException.class)
    public void testUnpublishedThrows() throws UnpublishedException {
        Environment.getPublished(Sys.class);
    }

    @Test
    public void testDispose() throws UnpublishedException {
        Sys sys = new Sys();
        Environment.publish(Sys.class, sys);
        assertFalse(sys.isDisposed());
        Environment.dispose();
        assertTrue(sys.isDisposed());
    }
}
