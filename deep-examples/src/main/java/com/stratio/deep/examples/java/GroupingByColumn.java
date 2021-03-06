/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.deep.examples.java;

import com.stratio.deep.config.DeepJobConfigFactory;
import com.stratio.deep.config.IDeepJobConfig;
import com.stratio.deep.context.DeepSparkContext;
import com.stratio.deep.rdd.CassandraJavaRDD;
import com.stratio.deep.testentity.TweetEntity;
import com.stratio.deep.testutils.ContextProperties;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

import java.util.List;

// !!Important!!

/**
 * Author: Emmanuelle Raffenne
 * Date..: 13-feb-2014
 */
public final class GroupingByColumn {

    private static final Logger LOG = Logger.getLogger(GroupingByColumn.class);

    private static List<Tuple2<String, Integer>> results;

    private GroupingByColumn() {
    }

    /**
     * Application entry point.
     *
     * @param args the arguments passed to the application.
     */
    public static void main(String[] args) {
        doMain(args);
    }

    /**
     * This is the method called by both main and tests.
     *
     * @param args
     */
    public static void doMain(String[] args) {
        String job = "java:groupingByColumn";

        String keyspaceName = "test";
        String tableName = "tweets";


        // Creating the Deep Context
        ContextProperties p = new ContextProperties(args);
        DeepSparkContext deepContext = new DeepSparkContext(p.getCluster(), job, p.getSparkHome(),
                new String[]{p.getJar()});

// Create a configuration for the RDD and initialize it
        IDeepJobConfig config = DeepJobConfigFactory.create(TweetEntity.class)
                .host(p.getCassandraHost()).cqlPort(p.getCassandraCqlPort()).rpcPort(p.getCassandraThriftPort())
                .keyspace(keyspaceName).table(tableName)
                .initialize();

// Creating the RDD
        CassandraJavaRDD rdd = deepContext.cassandraJavaRDD(config);

        // grouping
        JavaPairRDD<String, List<TweetEntity>> groups = rdd.groupBy(new Function<TweetEntity, String>() {
            @Override
            public String call(TweetEntity tableEntity) {
                return tableEntity.getAuthor();
            }
        });

// counting elements in groups
        JavaPairRDD<String, Integer> counts = groups.map(new PairFunction<Tuple2<String, List<TweetEntity>>, String,
                Integer>() {
            @Override
            public Tuple2<String, Integer> call(Tuple2<String, List<TweetEntity>> t) {
                return new Tuple2<String, Integer>(t._1(), t._2().size());
            }
        });

// fetching the results
        results = counts.collect();

        LOG.info("Este es el resultado con groupBy: ");
        for (Tuple2 t : results) {
            LOG.info(t._1() + ": " + t._2().toString());
        }

        deepContext.stop();
    }

    public static List<Tuple2<String, Integer>> getResults() {
        return results;
    }
}
