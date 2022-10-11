let chart = LightweightCharts.createChart(document.body, {
	width: (window.innerWidth - 8),
    height: (window.innerHeight - 8),
	layout: {
		backgroundColor: '#ffffff',
		textColor: '#000000',
	},
	grid: {
		vertLines: {
			color: 'rgba(197, 203, 206, 0.5)',
		},
		horzLines: {
			color: 'rgba(197, 203, 206, 0.5)',
		},
	},
	crosshair: {
		mode: LightweightCharts.CrosshairMode.Normal,
	},
	rightPriceScale: {
		borderColor: 'rgba(197, 203, 206, 0.8)'
	},
	timeScale: {
		borderColor: 'rgba(197, 203, 206, 0.8)',
		tickMarkFormatter: (time) => {
			let hh = new Date(time * 1000).getHours();
			hh < 10 ? hh = hh = '0' + hh : hh;
			let mm = new Date(time * 1000).getMinutes();
			mm < 10 ? mm = mm = '0' + mm : mm;
			let ss = new Date(time * 1000).getSeconds();
			ss < 10 ? ss = ss = '0' + ss : ss;
			return String(`${hh}:${mm}:${ss}`);
		},
	},
	localization: {
		locale: 'en-US',
		dateFormat: 'yyyy/MM/dd',
		priceFormatter: function(price) {
			return parseFloat(price).toFixed(8);
		}
	}
});

let candleSeries = chart.addCandlestickSeries({
  upColor: '#00ff00',
  downColor: '#ff0000',
  borderDownColor: 'rgba(255, 144, 0, 1)',
  borderUpColor: 'rgba(255, 144, 0, 1)',
  wickDownColor: '#ee1111',
  wickUpColor: '#11ee11',
});

// resize
window.addEventListener('resize', function() {
	const width = (window.innerWidth - 8);
    const height =  (window.innerHeight - 8);
    chart.resize(width, height);
	updateNodes(); //<<<< add here
});


// SetInitialData
const setInitialData = (data) => {
	candleSeries.setData(data);
}

// AppendData
// { time: '2020-11-26', open: 180, high: 180, low: 178, close: 179 }
const appendData = (bar) => {
	const parsedBar = JSON.parse(bar);
	parsedBar.time = parsedBar.time / 1000;
	candleSeries.update(parsedBar);
	updateNodes(); //<<<< add here
}

const changePrecision = (precision) => {
	java.log("Precision change: "+precision);
	chart.applyOptions({
		localization: {
			priceFormatter: function(price) {
				return parseFloat(price).toFixed(precision);
			}
		}
	});

}



// SetMarkers
// [{ time: '2020-10-01', position: 'aboveBar', color: '#f68410', shape: 'circle', text: 'buy' }]
// type SeriesMarkerPosition = 'aboveBar' | 'belowBar' | 'inBar';
// type SeriesMarkerShape = 'circle' | 'square' | 'arrowUp' | 'arrowDown';
const setMarkers = (markers) => {
	java.log(markers);
	let parsedMarkers = JSON.parse(markers);
	parsedMarkers = parsedMarkers.map(marker => {
		return {
			...marker,
			time: marker.time / 1000
		}
	});
	candleSeries.setMarkers(parsedMarkers);
}

// params: price: number
let priceLine = null;
const updatePriceLine = (price) => {
	if (priceLine) {
		candleSeries.removePriceLine(priceLine);
	}
	priceLine = candleSeries.createPriceLine({ price: price, color: 'green', lineWidth: 2, lineStyle: 1, axisLabelVisible: true, title: '', });
}

window.addEventListener('mousemove', () => updateNodes());
window.addEventListener('wheel', () => updateNodes());

let labels = [];

function updateNodes() {
	labels.forEach( l => drawNode(l.price, l.timestamp, l.node, l.direction)); //<<<< here
}

function drawNode (price, timestamp, node, direction) {
	const timeScale = chart.timeScale();
	let coordY = Math.floor(candleSeries.priceToCoordinate(price) + 8);
	let coordX =  Math.floor(timeScale.timeToCoordinate(timestamp) + 8);
	coordX = Math.floor(coordX - (node.clientWidth / 2));
	if (direction === 'sell' || direction === 'sellLimit') {
		coordY = coordY - node.clientHeight;
	}
	node.style.top = `${coordY}px`;
	node.style.left = `${coordX}px`;
}

const upArrowFilled = `
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path d="M8 10h-5l9-10 9 10h-5v10h-8v-10zm8 12h-8v2h8v-2z"/></svg>
`

const upArrow = `
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path d="M10 18v-10h-2.51l4.51-5.01 4.51 5.01h-2.51v10h-4zm-2 2h8v-10h5l-9-10-9 10h5v10zm8 2h-8v2h8v-2z"/></svg>
`

// add new label
// direction: string ;
function addLabel(id, price, timestamp, text, direction) {
	timestamp = timestamp / 1000;
    let newNode = document.createElement('div');
    newNode.classList.add('float');
    if (direction === 'buyLimit') {
		newNode.classList.add('up'); // <<<<< here
		newNode.innerHTML = upArrow + "<br>" + text; // <<<<< here
	} else if (direction === 'buy') {
		newNode.classList.add('up'); // <<<<< here
		newNode.innerHTML = upArrowFilled + "<br>" + text; // <<<<< here
	} else if (direction === 'sellLimit') {
		newNode.classList.add('down'); // <<<<< here
		newNode.innerHTML = text + "<br>" + upArrow; // <<<<< here
    } else {
        newNode.classList.add('down'); // <<<<< here
        newNode.innerHTML = text + "<br>" + upArrowFilled; // <<<<< here
    }
    document.body.append(newNode);
    drawNode(price, timestamp, newNode, direction);
    labels.push({id, node: newNode, price, timestamp, direction });
}



// remove label by id
function removeLabel(id) {
	let node = labels.find( l => l.id === id).node;
	node.remove();
	labels = labels.filter( l => l.id !== id);
}

// remove all labels
function removeLabels(id) {
	labels.forEach( l => l.node.remove());
	labels = [];
}

let canvases = document.querySelectorAll('canvas');
canvases[2].style.zIndex = 11;
canvases[3].style.zIndex = 12;
canvases[4].style.zIndex = 11;
canvases[5].style.zIndex = 12;