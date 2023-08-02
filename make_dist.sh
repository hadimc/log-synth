#!/bin/bash
TMP_DIR="/tmp/log-synth"
USER_SAMPLE_NO=100
BIO_SAMPLE_NO=1000
MAPPING_SAMPLE_NO=200
DATASET_SIZE=1000
THREADS=10

echo "-------------------------------------"
echo "Clearing temp directory ..."
echo "-------------------------------------"
mkdir -p $TMP_DIR
rm -rf $TMP_DIR/*

echo "-------------------------------------"
echo "Creating profile_dist.csv file ..."
echo "-------------------------------------"
./target/log-synth -count $USER_SAMPLE_NO -schema input/profile_schema.json -format CSV -quote OPTIMISTIC -output $TMP_DIR/
mv $TMP_DIR/synth-0000..csv src/main/resources/profile_dist.csv
#cp src/main/resources/profile_dist.csv target/classes/profile_dist.csv

echo "-------------------------------------"
echo "Creating location_dist.csv file ..."
echo "-------------------------------------"
./target/log-synth -count $USER_SAMPLE_NO -schema input/location_schema.json -template input/location_template.csv -format CSV -quote OPTIMISTIC -output  $TMP_DIR/
sed '1s/^.*$/external_ip/' $TMP_DIR/synth-0000..csv > src/main/resources/location_dist.csv
#cp src/main/resources/location_dist.csv target/classes/location_dist.csv

echo "-------------------------------------"
echo "Creating bio_dist.csv file ..."
echo "-------------------------------------"
./target/log-synth -count $BIO_SAMPLE_NO -schema input/bio_schema.json -template input/bio_template.csv -quote OPTIMISTIC -format CSV -output $TMP_DIR/
cp $TMP_DIR/synth-0000..csv src/main/resources/bio_dist.csv
#cp src/main/resources/bio_dist.csv target/classes/bio_dist.csv

echo "-------------------------------------"
echo "Creating device_dist.csv file ..."
echo "-------------------------------------"
./target/log-synth -count $USER_SAMPLE_NO -schema input/device_schema.json -template input/device_template.csv -quote OPTIMISTIC -format CSV -output $TMP_DIR/
cp $TMP_DIR/synth-0000..csv src/main/resources/device_dist.csv
#cp src/main/resources/device_dist.csv target/classes/device_dist.csv

echo "-------------------------------------"
echo "Recompling java source ..."
echo "-------------------------------------"
mvn install -DskipTests 1> /dev/null
sleep 5

echo "-------------------------------------"
echo "Creating profile_mapping_dist.json file"
echo "-------------------------------------"
./target/log-synth -count $MAPPING_SAMPLE_NO -schema input/mapping_schema.json -template input/mapping_template.csv -quote OPTIMISTIC -format CSV -output $TMP_DIR/
sed '1s/^.*$/profile_id,external_ip,key_strokes,device_os,screen_orientation,user_agent/'   $TMP_DIR/synth-0000..csv > src/main/resources/profile_mapping_dist.csv
#cp src/main/resources/profile_mapping_dist.csv target/classes/profile_mapping_dist.csv

echo "-------------------------------------"
echo "Recompling java source ..."
echo "-------------------------------------"
mvn install -DskipTests 1> /dev/null
sleep 5

echo "-------------------------------------"
echo "Creating dataset.json file"
echo "-------------------------------------"
java -Xmx14G -jar ./target/log-synth -count $DATASET_SIZE -threads $THREADS -schema input/full_schema.json -template input/template_small.json -quote OPTIMISTIC -format JSON -output $TMP_DIR/
cat $TMP_DIR/synth-*.json | sed -e 's/_____/\,/g' -e '1s/^.*$/\[\{/' -e '$s/^.*$/}]/' > output/full/dataset.json

rm -rf $TMP_DIR

