package src.main.server.io;

public class Throughput {
    private final long readThroughputDelay;

    private final long writeThroughputDelay;

    private long firstReadInstant;

    private long firstWroteInstant;

    private long lastReadInstant;

    private long lastWroteInstant;

    private long numberOfBytesRead;

    private long numberOfBytesWritten;

    public Throughput(long readThroughputDelay, long writeThroughputDelay) {
        this.readThroughputDelay = readThroughputDelay;
        this.writeThroughputDelay = writeThroughputDelay;
    }

    public synchronized long lastUsed() {
        if (lastReadInstant == 0 && lastWroteInstant == 0) {
            return Long.MAX_VALUE;
        }

        return Math.max(lastReadInstant, lastWroteInstant);
    }

    /**
     * Signals that some number of bytes were read from a client.
     *
     * @param numberOfBytes The number of bytes.
     */
    public synchronized void read(long numberOfBytes) {
        long now = System.currentTimeMillis();
        if (firstReadInstant == 0) {
            firstReadInstant = now;
        }

        numberOfBytesRead += numberOfBytes;
        lastReadInstant = now;
    }

    public synchronized long readThroughput(long now) {
        // Haven't read anything yet, or we read everything in the first read (instants are equal)
        if (firstReadInstant == 0 || numberOfBytesRead == 0 || lastReadInstant == firstReadInstant) {
            return Long.MAX_VALUE;
        }

        // No bytes have been written
        if (numberOfBytesWritten == 0) {
            long millis = now - firstReadInstant;
            // We are within the read delay and do not yet have enough data to make a meaningful calculation for actual throughput.
            if (millis < readThroughputDelay) {
                return Long.MAX_VALUE;
            }

            // Always zero
            return numberOfBytesWritten;
        }

        // The number of bytes read in seconds
        double result = ((double) numberOfBytesRead / (double) (lastReadInstant - firstReadInstant)) * 1_000;
        return Math.round(result);
    }

    public synchronized long writeThroughput(long now) {
        // Haven't written anything yet or not enough time has passed to calculated throughput (2s)
        if (firstWroteInstant == 0 || numberOfBytesWritten == 0) {
            return Long.MAX_VALUE;
        }

        // Always use currentTime since this calculation is ongoing until the client reads all the bytes
        long millis = now - firstWroteInstant;
        if (millis < writeThroughputDelay) {
            return Long.MAX_VALUE;
        }

        double result = ((double) numberOfBytesWritten / (double) millis) * 1_000;
        return Math.round(result);
    }

    /**
     * Signals that some number of bytes were wrote to a client.
     *
     * @param numberOfBytes The number of bytes.
     */
    public synchronized void wrote(long numberOfBytes) {
        long now = System.currentTimeMillis();
        if (firstWroteInstant == 0) {
            firstWroteInstant = now;
        }

        numberOfBytesWritten += numberOfBytes;
        lastWroteInstant = now;
    }
}
