package com.stratio.deep.embedded;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.stratio.deep.util.Constants;

public class CassandraServer {

	public static final int CASSANDRA_THRIFT_PORT = 9360;
	public static final int CASSANDRA_CQL_PORT = 9242;
	
	private static final Logger logger = Logger
			.getLogger(CassandraServer.class);
	private static final int WAIT_SECONDS = 3;

	private final String yamlFilePath;
	private CassandraDaemon cassandraDaemon;
	private String[] startupCommands;

	public CassandraServer() {
		this("/cassandra.yaml");
	}

	public CassandraServer(String yamlFile) {
		this.yamlFilePath = yamlFile;
	}

	static ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * Set embedded cassandra up and spawn it in a new thread.
	 * 
	 * @throws TTransportException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void start() throws IOException,
			InterruptedException, ConfigurationException {

		File dir = Files.createTempDir();
		String dirPath = dir.getAbsolutePath();
		System.out.println("Storing Cassandra files in " + dirPath);

		URL url = Resources.getResource("cassandra.yaml");
		String yaml = Resources.toString(url, Charsets.UTF_8);
		yaml = yaml.replaceAll("REPLACEDIR", dirPath);
		String yamlPath = dirPath + File.separatorChar + "cassandra.yaml";
		org.apache.commons.io.FileUtils.writeStringToFile(new File(yamlPath),
				yaml);

		// make a tmp dir and copy cassandra.yaml and log4j.properties to it
		copy("/log4j.properties", dir.getAbsolutePath());
		System.setProperty("cassandra.config", "file:" + dirPath + yamlFilePath);
		System.setProperty("log4j.configuration", "file:" + dirPath
				+ "/log4j.properties");
		System.setProperty("cassandra-foreground", "true");

		cleanupAndLeaveDirs();

		try {
			executor.execute(new CassandraRunner());
		} catch (RejectedExecutionException e) {
			logger.error(e);
			return;
		}

		try {
			TimeUnit.SECONDS.sleep(WAIT_SECONDS);
		} catch (InterruptedException e) {
			logger.error(e);
			throw new AssertionError(e);
		}

		initKeySpace();
	}

	private void initKeySpace() {
		if (startupCommands == null || startupCommands.length == 0) {
			return;
		}
		Cluster cluster = Cluster.builder().withPort(CASSANDRA_CQL_PORT)
				.addContactPoint(Constants.DEFAULT_CASSANDRA_HOST).build();
		Session session = cluster.connect();

		try {
			for (String command : startupCommands) {
				try {

					if (StringUtils.isNotEmpty(command)) {
						session.execute(command);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			session.shutdown();
		}

	}

	public void shutdown() throws IOException {
		executor.shutdown();
		executor.shutdownNow();
	}

	/**
	 * Copies a resource from within the jar to a directory.
	 * 
	 * @param resource
	 * @param directory
	 * @throws IOException
	 */
	private static void copy(String resource, String directory)
			throws IOException {
		mkdir(directory);
		InputStream is = CassandraServer.class.getResourceAsStream(resource);
		String fileName = resource.substring(resource.lastIndexOf("/") + 1);
		File file = new File(directory + System.getProperty("file.separator")
				+ fileName);
		OutputStream out = new FileOutputStream(file);
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
	}

	/**
	 * Creates a directory
	 * 
	 * @param dir
	 * @throws IOException
	 */
	private static void mkdir(String dir) throws IOException {
		FileUtils.createDirectory(dir);
	}

	private static void cleanupAndLeaveDirs() throws IOException {
		mkdirs();
		cleanup();
		mkdirs();
		CommitLog.instance.resetUnsafe(); // cleanup screws w/ CommitLog, this
		// brings it back to safe state
	}

	private static void cleanup() throws IOException {

		// clean up commitlog
		String[] directoryNames = { DatabaseDescriptor.getCommitLogLocation(), };
		for (String dirName : directoryNames) {
			File dir = new File(dirName);
			if (!dir.exists()) {
				logger.error("No such directory: " + dir.getAbsolutePath());
				throw new RuntimeException("No such directory: "
						+ dir.getAbsolutePath());
			}
			FileUtils.deleteRecursive(dir);
		}

		// clean up data directory which are stored as data directory/table/data
		// files
		for (String dirName : DatabaseDescriptor.getAllDataFileLocations()) {
			File dir = new File(dirName);
			if (!dir.exists()) {
				logger.error("No such directory: " + dir.getAbsolutePath());
				throw new RuntimeException("No such directory: "
						+ dir.getAbsolutePath());
			}
			FileUtils.deleteRecursive(dir);
		}
	}

	private static void mkdirs() {
		DatabaseDescriptor.createAllDirectories();
	}

	private class CassandraRunner implements Runnable {
		@Override
		public void run() {
			cassandraDaemon = new CassandraDaemon();
			cassandraDaemon.activate();
		}
	}

	public String[] getStartupCommands() {
		return startupCommands;
	}

	public void setStartupCommands(String[] startupCommands) {
		this.startupCommands = startupCommands;
	}
}
