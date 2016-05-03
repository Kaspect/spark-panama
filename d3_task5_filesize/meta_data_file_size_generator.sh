TIKA_APP=/home/hangguo/cs599/Tika/trunk/tika-app/target/tika-app-1.12-SNAPSHOT.jar
FILE_LIST_PREFIX=/home/hangguo/crawl/FILE_LIST

#for filetype in pdf txt html
for filetype in xhtml
do
	file_list="$FILE_LIST_PREFIX/file_list_100$filetype"
	output=meta_file_size_comp_$filetype
	rm $output
	printf "#file_path	#file_size(bytes)	#meta_data_size(bytes)\n" > $output
	while read fn
	do
		fn_json=$fn'.json'
		file_size="$(du -b $fn | sed 's/\.\/.*//')"
		meta_data_size="$(du -b $fn_json | sed 's/\.\/.*//')"
		printf "$fn	$file_size	$meta_data_size\n" >> $output		
	done < $file_list
done
