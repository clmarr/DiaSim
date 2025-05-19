const sendline = (option, input) => {
    console.log(option)
    fetch('/sendline', {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({"return":option.return })
    })
        .then(response => response.json())
        .then(data => {

            if (Array.isArray(data)) {
                data.forEach((element, index) => {
                    console.log(`Element ${index}:`, element);
                    input.nextElement(element[0], element[1])
                    if (input.updateData) {
                        input.updateData(data)
                    }
                });
            } else {
                console.log('Success:', data);
            }
        })
        .catch(error => {
            console.log(option)
            console.error("Error:", error)
        }); 

}
export { sendline }