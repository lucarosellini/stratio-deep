/*
 * Copyright 2014, Luca Rosellini.
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

package kina.examples.java;

import com.google.common.collect.Lists;

import kina.config.CassandraConfigFactory;
import kina.config.CassandraKinaConfig;
import kina.context.CassandraKinaContext;
import kina.rdd.CassandraJavaRDD;
import kina.testentity.TweetEntity;
import kina.testutils.ContextProperties;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

import java.util.List;

/**
 * Author: Emmanuelle Raffenne
 * Date..: 14-feb-2014
 */
public final class GroupingByKey {
    private static final Logger LOG = Logger.getLogger(GroupingByKey.class);
    private static List<Tuple2<String, Integer>> result;
    private static int authors;
    private static int total;

    private GroupingByKey() {
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
        String job = "java:groupingByKey";

        String keyspaceName = "test";
        String tableName = "tweets";

        // Creating the Kina Context where args are Spark Master and Job Name
        ContextProperties p = new ContextProperties(args);
	    CassandraKinaContext kinaContext = new CassandraKinaContext(p.getCluster(), job, p.getSparkHome(), p.getJars());


        // Creating a configuration for the RDD and initialize it
        CassandraKinaConfig<TweetEntity> config = CassandraConfigFactory.create(TweetEntity.class)
                .host(p.getCassandraHost()).cqlPort(p.getCassandraCqlPort()).rpcPort(p.getCassandraThriftPort())
                .keyspace(keyspaceName).table(tableName)
                .initialize();

        // Creating the RDD
        CassandraJavaRDD<TweetEntity> rdd = (CassandraJavaRDD) kinaContext.cassandraJavaRDD(config);

        // creating a key-value pairs RDD
        JavaPairRDD<String, TweetEntity> pairsRDD = rdd.mapToPair(new PairFunction<TweetEntity, String, TweetEntity>() {
            @Override
            public Tuple2<String, TweetEntity> call(TweetEntity t) {
                return new Tuple2<String, TweetEntity>(t.getAuthor(), t);
            }
        });

// grouping
        JavaPairRDD<String, Iterable<TweetEntity>> groups = pairsRDD.groupByKey();

// counting elements in groups
        JavaPairRDD<String, Integer> counts = groups.mapToPair(new PairFunction<Tuple2<String,
                Iterable<TweetEntity>>, String,
                Integer>() {
            @Override
            public Tuple2<String, Integer> call(Tuple2<String, Iterable<TweetEntity>> t) {
                return new Tuple2<String, Integer>(t._1(), Lists.newArrayList(t._2()).size());
            }
        });

// fetching results
        result = counts.collect();

        LOG.info("Este es el resultado con groupByKey: ");
        total = 0;
        authors = 0;
        for (Tuple2<String, Integer> t : result) {
            total = total + t._2();
            authors = authors + 1;
            LOG.info(t._1() + ": " + t._2().toString());
        }

        LOG.info("Autores: " + authors + " total: " + total);

        kinaContext.stop();
    }

    public static List<Tuple2<String, Integer>> getResult() {
        return result;
    }

    public static int getAuthors() {
        return authors;
    }

    public static int getTotal() {
        return total;
    }
}
