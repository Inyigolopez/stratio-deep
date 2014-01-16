package org.apache.cassandra.hadoop.cql3;

import org.apache.cassandra.hadoop.AbstractColumnFamilyOutputFormat;
import org.apache.commons.lang.NotImplementedException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Custom implementation of Hadoop outformat returning a DeepCqlRecordWriter
 */
public class DeepCqlOutputFormat extends AbstractColumnFamilyOutputFormat<Map<String, ByteBuffer>, List<ByteBuffer>> {

    /**
     * Returns a DeepCqlRecordWriter.
     *
     * @param context
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public CqlRecordWriter getRecordWriter(final TaskAttemptContext context) throws IOException, InterruptedException {
        return new CqlRecordWriter(context);
    }

    /** Fills the deprecated OutputFormat interface for streaming. */
    @Override
    @Deprecated
    public CqlRecordWriter getRecordWriter(FileSystem fileSystem, JobConf entries, String s, Progressable progressable) throws IOException {
        throw new NotImplementedException("Deprecated method \'getRecordWriter(FileSystem fileSystem, JobConf entries, String s, Progressable progressable)\' not implemented");
    }
}
