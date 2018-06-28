/*
 *  Copyright 2017-2018 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.twosigma.flint.timeseries.summarize.summarizer.subtractable

import breeze.numerics.abs
import com.twosigma.flint.timeseries.{ Summarizers, TimeSeriesGenerator, Windows }
import com.twosigma.flint.timeseries.row.Schema
import com.twosigma.flint.timeseries.summarize.SummarizerSuite
import org.apache.spark.sql.types.{ DoubleType, IntegerType }

class NthMomentSummarizerSpec extends SummarizerSuite {

  override val defaultResourceDir: String = "/timeseries/summarize/summarizer/nthmomentsummarizer"

  "NthMomentSummarizer" should "`computeNthMoment` correctly" in {
    val priceTSRdd = fromCSV("Price.csv", Schema("id" -> IntegerType, "price" -> DoubleType))
    var results = priceTSRdd.summarize(Summarizers.nthMoment("price", 0), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_0thMoment") === 1.0)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_0thMoment") === 1.0)

    results = priceTSRdd.summarize(Summarizers.nthMoment("price", 1), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_1thMoment") === 3.0833333333333335)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_1thMoment") === 3.416666666666667)

    results = priceTSRdd.summarize(Summarizers.nthMoment("price", 2), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_2thMoment") === 12.041666666666668)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_2thMoment") === 15.041666666666666)

    results = priceTSRdd.summarize(Summarizers.nthMoment("price", 3), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_3thMoment") === 53.39583333333333)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_3thMoment") === 73.35416666666667)

    results = priceTSRdd.summarize(Summarizers.nthMoment("price", 4), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_4thMoment") === 253.38541666666669)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_4thMoment") === 379.0104166666667)
  }

  it should "ignore null values" in {
    val priceTSRdd =
      fromCSV("Price.csv", Schema("id" -> IntegerType, "price" -> DoubleType))
    assertEquals(
      priceTSRdd.summarize(Summarizers.nthMoment("price", 0), Seq("id")),
      insertNullRows(priceTSRdd, "price").summarize(Summarizers.nthMoment("price", 0), Seq("id"))
    )
  }

  it should "pass summarizer property test" in {
    summarizerPropertyTest(AllPropertiesAndSubtractable)(Summarizers.nthMoment("x1", 1))
    summarizerPropertyTest(AllPropertiesAndSubtractable)(Summarizers.nthMoment("x2", 2))
  }

  "NthCentralMomentSummarizer" should "`computeNthCentralMoment` correctly" in {
    val priceTSRdd = fromCSV("Price.csv", Schema("id" -> IntegerType, "price" -> DoubleType))
    var results = priceTSRdd.summarize(Summarizers.nthCentralMoment("price", 1), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_1thCentralMoment") === 0d)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_1thCentralMoment") === 0d)

    results = priceTSRdd.summarize(Summarizers.nthCentralMoment("price", 2), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_2thCentralMoment") === 2.534722222222222)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_2thCentralMoment") === 3.3680555555555554)

    results = priceTSRdd.summarize(Summarizers.nthCentralMoment("price", 3), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_3thCentralMoment") === 0.6365740740740735)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_3thCentralMoment") === -1.0532407407407405)

    results = priceTSRdd.summarize(Summarizers.nthCentralMoment("price", 4), Seq("id")).collect()
    assert(results.find(_.getAs[Int]("id") == 3).head.getAs[Double]("price_4thCentralMoment") === 10.567563657407407)
    assert(results.find(_.getAs[Int]("id") == 7).head.getAs[Double]("price_4thCentralMoment") === 21.227285879629633)
  }

  it should "return 0.0 for variance with constant values" in {
    val dataWithConstantColumn = AllData(1).addColumns("c" -> DoubleType -> { _ => 1.0 })
    var results = dataWithConstantColumn.summarize(
      Summarizers.nthCentralMoment("c", 2)
    ).collect().head
    assert(results.getAs[Double]("c_2thCentralMoment") == 0.0)

    results = dataWithConstantColumn.summarizeWindows(
      Windows.pastAbsoluteTime("10000 ns"),
      Summarizers.nthCentralMoment("c", 2)
    ).collect().last
    assert(results.getAs[Double]("c_2thCentralMoment") == 0.0)
  }

  it should "return almost zero for variance with constant values in a window" in {
    val dataWithRandomColumn = new TimeSeriesGenerator(
      sc,
      begin = 0L,
      end = 100000L,
      frequency = 10L
    )(
      uniform = false,
      ids = Seq(1),
      ratioOfCycleSize = 1.0,
      columns = Seq(
        "x" -> { (_: Long, _: Int, rand: util.Random) =>
          rand.nextDouble()
        }
      ),
      numSlices = 1
    ).generate()
    val dataWithConstantColumn = new TimeSeriesGenerator(
      sc,
      begin = 100000L,
      end = 200000L,
      frequency = 10L
    )(
      uniform = false,
      ids = Seq(1),
      ratioOfCycleSize = 1.0,
      columns = Seq(
        "x" -> { (_: Long, _: Int, rand: util.Random) =>
          1.0
        }
      ),
      numSlices = 1
    ).generate()
    val testData = dataWithRandomColumn.merge(dataWithConstantColumn)

    val results = testData.summarizeWindows(
      Windows.pastAbsoluteTime("1000 ns"),
      Summarizers.nthCentralMoment("x", 2)
    ).collect().last

    // |observations| * variance
    assert(abs(100 * results.getAs[Double]("x_2thCentralMoment")) < 1.0E-12)
  }

  it should "pass summarizer property test" in {
    summarizerPropertyTest(AllPropertiesAndSubtractable)(Summarizers.nthCentralMoment("x2", 2))
    summarizerPropertyTest(AllPropertiesAndSubtractable)(Summarizers.nthCentralMoment("x3", 3))
  }
}
