TIKA_APP=/home/hangguo/cs599/Tika/trunk/tika-app/target/tika-app-1.12-SNAPSHOT.jar
FILE_LIST_PREFIX=/home/hangguo/crawl/FILE_LIST

#for filetype in xhtml pdf txt html
for filetype in xhtml
do
	file_list="$FILE_LIST_PREFIX/file_list_100$filetype"
	while read fn
	do
		fn_json=$fn'.json'
	        java -jar $TIKA_APP -j $fn > $fn_json
	done < $file_list
done
