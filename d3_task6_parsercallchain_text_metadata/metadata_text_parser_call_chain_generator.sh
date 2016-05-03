TIKA_APP=/home/hangguo/cs599/Tika/trunk/tika-app/target/tika-app-1.12-SNAPSHOT.jar
FILE_LIST_PREFIX=/home/hangguo/crawl/FILE_LIST

for filetype in xhtml pdf txt html
#for filetype in xhtml
do
        file_list="$FILE_LIST_PREFIX/file_list_100$filetype"
        output='metadata_text_parser_call_chain_'$filetype
        rm $output
        printf "#file_path	#file_size(bytes)	#meta_data_size(bytes)	#text_data_size(bytes)	#paser_call_chain\n" > $output
        while read fn
        do
                fn_json=$fn'.json'
		fn_txt=$fn'.txt'
                file_size="$(du -b $fn | sed 's/\.\/.*//')"
                meta_data_size="$(du -b $fn_json | sed 's/\.\/.*//')"
		text_data_size="$(du -b $fn_txt | sed 's/\.\/.*//')"
		parser_call_chain="$(java -jar $TIKA_APP $fn grep -i | X-Parsed-By | sed 's/^.*content="//' | sed 's/"\/>//' | sed 'N;s/\n/, /')"
                printf "$fn	$file_size	$meta_data_size	$text_data_size	$parser_call_chain\n" >> $output
        done < $file_list
done
