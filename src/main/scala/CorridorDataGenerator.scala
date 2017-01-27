import java.time.format.DateTimeFormatter
import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

import com.sksamuel.elastic4s.{ ElasticClient, ElasticsearchClientUri }
import com.sksamuel.elastic4s.bulk._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.bulk.BulkCompatibleDefinition
import com.sksamuel.elastic4s.mappings.FieldType._
import org.elasticsearch.common.settings.Settings

import scala.concurrent.duration._
import scala.util.Random

case class CorridorSpeedData(corridor: String, datetime: ZonedDateTime, speed: Double)

case class Corridor(name: String, length: Double, rushHourSpeed: Double, regularSpeed: Double)

object CorridorDataGenerator extends App {

  val host = "boot2docker"
  val port = 9300
  val eClusterName = ""
  val settings = Settings.builder().put("cluster.name", "elasticsearch").build()


  val client = ElasticClient.transport(settings, ElasticsearchClientUri(host, port))

  def deleteCorridorIndex() = {
    client.execute {
      deleteIndex("traffic")
    }.await(10 seconds)
  }

  def createCorridorIndex() = {
    client.execute {
      createIndex("traffic").mappings(
        mapping("speed") as(
          field("speed", DoubleType),
          field("datetime", DateType),
          field("corridor", TextType),
          field("traveltime", DoubleType)
          )
      )
    }.await(10 seconds)
  }

  def generateCorridorData(numdays: Int, startDate: LocalDate, corridors: Corridor*) = {
    def isSpits(time: ZonedDateTime) = (time.toLocalTime.isAfter(LocalTime.of(15, 30)) && time.toLocalTime.isBefore(LocalTime.of(19, 0))) || (time.toLocalTime.isAfter(LocalTime.of(7, 0)) && time.toLocalTime.isBefore(LocalTime.of(10, 0)))
    val start = startDate.atStartOfDay(ZoneId.systemDefault())

    def loop(current: ZonedDateTime, corridor: Corridor, bul: List[com.sksamuel.elastic4s.indexes.IndexDefinition]): List[com.sksamuel.elastic4s.indexes.IndexDefinition] = {
      if (current.compareTo(startDate.plusDays(numdays).atStartOfDay(ZoneId.systemDefault())) < 0) {
        print(corridor.name)
        val speed = if (isSpits(current)) {
          Math.abs(corridor.rushHourSpeed + 5 * Random.nextGaussian())
        } else {
          Math.abs(corridor.regularSpeed + 10 * Random.nextGaussian())
        }

        //        client.execute(indexInto("traffic", "speed") fields("corridor" → "A", "@timestamp" → current.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "speed" → speed, "traveltime" → (corridor.length / speed))).await
        val nd = indexInto("traffic", "speed") fields("corridor" → corridor.name, "@timestamp" → current.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "speed" → speed, "traveltime" → (corridor.length / speed))
        loop(current.plusMinutes(1), corridor, nd :: bul)
      } else {
        bul
      }
    }


    corridors.foreach { corridor ⇒
      val bul = loop(start, corridor, Nil)
      client.execute(bulk(bul)).await
    }
  }

  deleteCorridorIndex()
  createCorridorIndex()
  generateCorridorData(3*7, LocalDate.now().minusWeeks(3), Corridor("A", 10, 30, 50), Corridor("B", 7, 10, 100))
//  generateCorridorData(3*7, LocalDate.now().minusWeeks(3), Corridor("B", 7, 10, 100))

}
