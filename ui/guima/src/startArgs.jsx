import React, { useState } from 'react';
import './startArgs.css'; // Assuming you'll create a CSS file for styling
import Menu from './Menu';

const menuItems = [
    { "title": "lex", "type": "file", "hint": "Sets the file with the etyma to implement sound changes on" },
    { "title": "rules", "type": "file", "hint": "Sets the file with the ordered sound changes to realize upon the lexicon" },
    { "title": "out", "type": "string", "hint": "Name for the output folder with all resulting forward-reconstructions and analysis files" },
    { "title": "symbols", "type": "file", "hint": "Symbol definitions file (optional, default: symbolDefs.csv)" },
    { "title": "impl", "type": "file", "hint": "Feature implications file (optional, default: FeatureImplications)" },
    { "title": "diacrit", "type": "file", "hint": "Custom diacritics file (optional)" },
    { "title": "idcost", "type": "int", "hint": "Cost of insertion and deletion for computing edit distances" },
    { "title": "verbose", "type": "bool", "hint": "Prints more information about file locations and other variables" },
    { "title": "print", "type": "bool", "hint": "Print changes mode - prints words changed by each rule" },
    { "title": "halt", "type": "bool", "hint": "Halt mode - halts at all intermediate stages" },
    { "title": "explict", "type": "bool", "hint": "Explicit mode - ignores feature implications" },
    { "title": "skip", "type": "bool", "hint": "Skip file creation - runs without creating output folder" }
];
const tempPlaceHolder = [
    { "lex": "press to search for file" },
    { "rules": "press to search for file" },
    { "out": "press to search for file" },
    { "symbols": "press to search for file" },
    { "impl": "press to search for file" },
    { "diacrit": "press to search for file" },
];

async function getFile(key, placeHolders, setPlaceholders) {
    try {
        const response = await fetch(`/filename`);
        const data = await response.json();
        const newPlaceHolders = { ...placeHolders, [key]: data["filename"] };
        console.log(newPlaceHolders[key]);
        setPlaceholders(newPlaceHolders);
    } catch (error) {
        console.error('Error fetching file:', error);
    }
}

const StartMenu = (input) => {
    console.log(input);
    const [formData, setFormData] = useState(input.data || {});
    const [placeHolders, setPlaceholders] = useState(() => {
        // Load saved placeholders from localStorage on initialization
        const savedPlaceholders = localStorage.getItem('diasimPlaceholders');
        return savedPlaceholders ? JSON.parse(savedPlaceholders) : {};
    });

    // Save to localStorage whenever placeHolders changes
    React.useEffect(() => {
        localStorage.setItem('diasimPlaceholders', JSON.stringify(placeHolders));
    }, [placeHolders]);

    const handleInputChange = (title, value) => {
        const newPlaceholders = { ...placeHolders, [title]: value };
        setPlaceholders(newPlaceholders);
        // Update local state
        setFormData({
            ...formData,
            [title]: value
        });


    };

    const handleSubmit = (e) => {
        e.preventDefault();
        console.log('Form submitted with data:', formData);
        // Send the updated form data to the server
        fetch('/start/config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                ...formData,
                ...Object.fromEntries(
                    Object.entries(placeHolders).filter(([_, v]) => v && v !== 'press to search for file')
                )
            }),
        })
            .then(response => response.json())
            .then(data => {
                // console.log('Success:', data);
                input.nextElement(Menu, data);
            })
            .catch((error) => {
                console.error('Error:', error);
            });
        // Use nextElement instead of input.addElement to navigate to the Menu component
        // Update data if needed
        if (input.updateData) {
            input.updateData(formData);
        }
    };

    const renderInput = (item) => {
        switch (item.type) {
            case 'file':
                return (
                    <>
                        <input
                            type="text"
                            id={item.title}
                            value={placeHolders[item.title] || ''}
                            onChange={(e) => handleInputChange(item.title, e.target.value)}
                            placeholder={`Select ${item.title} file...`}
                            disabled={!input.isActive ? true : false}
                        />
                        <button 
                            onClick={(e) => {
                                e.preventDefault();
                                getFile(item.title, placeHolders, setPlaceholders);
                            }} 
                            disabled={!input.isActive ? true : false}>
                            Browse
                        </button>
                    </>
                );
            case 'string':
                return (
                    <>
                        <input
                            type="text"
                            id={item.title}
                            value={formData[item.title] || ''}
                            onChange={(e) => handleInputChange(item.title, e.target.value)}
                            disabled={!input.isActive ? true : false}
                        />
                    </>
                );
            case 'int':
                return (
                    <input
                        type="number"
                        id={item.title}
                        value={formData[item.title] || ''}
                        onChange={(e) => handleInputChange(item.title, parseInt(e.target.value, 10))}
                        disabled={!input.isActive ? true : false}
                    />
                );
            case 'bool':
                return (
                    <input
                        type="checkbox"
                        id={item.title}
                        checked={formData[item.title] || false}
                        onChange={(e) => handleInputChange(item.title, e.target.checked)}
                        disabled={!input.isActive ? true : false}
                    />
                );
            default:
                return (
                    <input
                        type="text"
                        id={item.title}
                        value={formData[item.title] || ''}
                        onChange={(e) => handleInputChange(item.title, e.target.value)}
                    />
                );
        }
    };

    return (
        <div className="start-menu">
            <h1>DiaSim Configuration</h1>
            <form id='mainForm'>
                {menuItems.map((item) => (
                    <div className="menu-item" key={item.title}>
                        <label htmlFor={item.title} title={item.hint}>
                            {item.title}:
                        </label>
                        {renderInput(item)}
                        <small className="hint">{item.hint}</small>
                    </div>
                ))}
                <button type="submit" className="submit-btn" disabled={!input.isActive ? true : false} onClick={(e) => { e.preventDefault(); handleSubmit(e) }}>Start Simulation</button>
            </form>
        </div>
    );
};

export default StartMenu;