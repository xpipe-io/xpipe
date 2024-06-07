for arg in "$@"
do
    file="arg"
    temp_file=$(mktemp)
    awk '{ sub("\r$", ""); print }' "$file" "$temp_file"
    cat "$temp_file" > "file"
    rm "$temp_file"
done
