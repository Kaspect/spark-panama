/* SimpleApp.scala */
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import com.amazonaws.services.s3._, model._
import com.amazonaws.auth.BasicAWSCredentials

object SimpleApp {
  def get_keys_from_s3_bucket(key: String, secret: String, bucketName: String, maxKeys: Int)= {
    val request = new ListObjectsRequest()
    request.setBucketName(bucketName)
    request.setPrefix("")
    request.setMaxKeys(maxKeys)

    def s3 = new AmazonS3Client(new BasicAWSCredentials(key, secret))
    val objs = s3.listObjects(request)
//    val keys = objs.getObjectSummaries.toList.map(x => x.getKey)
    val keys = objs.getObjectSummaries().toArray
//    keys(0).asInstanceOf[S3ObjectSummary].getSize()
  	keys
  }
  def download_file_from_s3(sparkContext: SparkContext, key: String, secret: String, bucketName: String, s3_file_key: String) = {
    val data = sparkContext.textFile("s3n://" + key + ":" + secret + "@" + bucketName + s3_file_key)
    data
  }
  def main(args: Array[String]) {
  	val awsKey = "AKIAIYQGAK2RABXX2GKA"
  	val awsSecret = "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"
  	val bucket = "latest-commoncrawl"
  	val keys = get_keys_from_s3_bucket(awsKey, awsSecret, bucket, 100)
  	def s3 = new AmazonS3Client(new BasicAWSCredentials(awsKey, awsSecret))
//    val firstKey = keys(0).asInstanceOf[S3ObjectSummary].getKey
    val key_string_list = keys.map(x => x.asInstanceOf[S3ObjectSummary].getKey())
//
////  	val objects = keys.map(key => s3.getObject(bucket, key))
//  	// val quickobj = s3.getObject("latest-commoncrawl","crawl/572-team1.json")
//    val conf = new SparkConf().setAppName("Simple Application")
//    val sc = new SparkContext(conf)
    download_file_from_s3(sc,awsKey,awsSecret,bucket, "crawl/572-team1.json")
//    println(firstKey)
    val x = 0



  }
}


    // val objs = s3.listObjects(request) // Note that this method returns truncated data if longer than the "pageLength" above. You might need to deal with that.
    // objs.getObjectSummaries.map(_.getKey).toList)
    //     .flatMap { key => Source.fromInputStream(s3.getObject(bucket, key).getObjectContent: InputStream).getLines }

