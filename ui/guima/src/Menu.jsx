import React from "react";
import { sendline } from "./utils";

const buttonClick = async (event, input, option) => {
    event.preventDefault();
    sendline(option, input)
}
const Menu = (input) => {

    const processMenuItem = (option) => {
        switch (option.type) {
            case "button":
                return (
                    <>
                        <button onClick={(e) => {buttonClick(e, input, option) }} disabled={!input.isActive}>{option.text}</button>
                        </>
                )
            case "number":
                return (
                    <>
                        <input placeholder={option.text}></input>
                        <button onClick={(e) => {buttonClick(e, input, option)}} disabled={!input.isActive}>Submit</button>
                    </>
                )
            default:
                return (
                    <div>Something got messed up got type: {option.type}</div>
                )
        }
    }
    return (
        <form>
            {input.data.map((items) => (
                processMenuItem(items)
            ))}
        </form>
    )
}

export default Menu