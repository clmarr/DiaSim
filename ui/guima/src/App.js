import logo from './logo.svg';
import './App.css';
import StartMenu from './startArgs';
import Menu from './Menu'
import { useState } from 'react';


function App() {
  const [elements, setElements] = useState([])
  const [index, setIndex] = useState(0)
  const [data, setData] = useState({})  // Add state for your data

  const addElement = (element, elementData = {}) => {
    setElements([...elements, element])
    setData({...data, [index]: elementData})  // Store data for this element
    setIndex(index + 1)
    // console.log(data);
  }
  // const nextElement = () => {
  //   const fetchUntilExit = () => {
  //     fetch("/next")
  //     .then(resp => resp.json())
  //     .then(data => {
  //       if (data.exit) {
  //       console.log("Exit condition reached:", data.exit);
  //       } else {
  //       // Process data then fetch again
  //       addElement(Menu, data);
  //       fetchUntilExit();
  //       }
  //     })
  //     .catch(error => {
  //       console.error("Error in fetch:", error);
  //     });
  //   };
    
  //   fetchUntilExit();
  // }

  // Initialize with StartMenu
  if (elements.length === 0) {
    addElement(StartMenu)
    console.log(index)
  }

  return (
    <div className="App">
      <header className="App-header">
        {elements.map((Component, ind) => (
          <div key={ind} style={{ display: ind < index ? 'block' : 'none' }}>
            <Component 
              isActive={ind === index - 1} 
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
