import logo from './logo.svg';
import './App.css';
import StartMenu from './startArgs';
import Menu from './Menu'
import { useEffect, useState } from 'react';
import { myText } from './text';


function App() {
  const [elements, setElements] = useState([])
  const [data, setData] = useState([])  // Use array as a queue instead of map
  const stringMap = { "start args": StartMenu, "menu": Menu, "text": myText }
  const addElement = (element, elementData = {}) => {
    setElements(prev => [...prev, stringMap[element]])
    setData(prev => [...prev, elementData])  // Store data for this element
  }
  

  // Initialize with StartMenu
  if (elements.length === 0) {
    addElement("start args")
  }

  return (
    <div className="App">
      <header className="App-header">
        {elements.map((Component, ind) => (
          <div key={ind} style={{ display: 'block' }}>
            <Component 
              isActive={ind === elements.length - 1} 
              nextElement={addElement} 
              data={data[ind] || {}}
            />
          </div>
        ))}
      </header>
    </div>
  );
}
export default App;
