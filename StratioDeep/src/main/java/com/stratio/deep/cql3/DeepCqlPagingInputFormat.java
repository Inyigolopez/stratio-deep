package com.stratio.deep.cql3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.cassandra.hadoop.cql3.CqlPagingInputFormat;
import org.apache.cassandra.hadoop.cql3.CqlPagingRecordReader;
import org.apache.cassandra.hadoop.cql3.IterableCqlPagingRecordReader;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * {@link CqlPagingRecordReader} implementation that returns an instance of a
 * {@link DeepCqlPagingRecordReader}.
 * 
 * @author Luca Rosellini <luca@strat.io>
 * 
 */
public class DeepCqlPagingInputFormat extends CqlPagingInputFormat {

    /**
     * Returns a new instance of {@link DeepCqlPagingRecordReader}.
     */
    @Override
    public RecordReader<Map<String, ByteBuffer>, Map<String, ByteBuffer>> createRecordReader(InputSplit arg0,
	    TaskAttemptContext arg1) throws IOException, InterruptedException {

	return new IterableCqlPagingRecordReader();
    }

}
