<?php

// This script runs as the `http` user.

$results = $_POST["results"];
$separator = "\n\n// ===========================================================================\n\n";

$file = "/path/to/this/repo/training-materials/questionaire/answers/" . $_POST["file"];
$contents = json_encode($results) . $separator;

file_put_contents($file, $contents, FILE_APPEND);

?>
