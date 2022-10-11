// generate initial values
const getCustomDate = (i) => {
    let result = new Date();
    result.setDate(result.getDate() + i);

    let dd = result.getDate();
    if (dd < 10) dd = '0' + dd;

    let mm = result.getMonth() + 1;
    if (mm < 10) mm = '0' + mm;

    return result.getFullYear() + '-' + mm + '-' + dd;
}

const generateChart = (items) => {
    const renko = [];
    let trend = true;
    let up = true;
    let s = 500; 
    
    for (let i = 0; i < items; i++) {
        if (Math.random() > 0.7) trend = !trend;
        if (trend) {
            if (!up) s = s + 10;
            up = true;
            renko.push({
                time: getCustomDate(i),
                open: s,
                high: s + 10,
                low: s,
                close: s + 10
            });
            s = s + 10;
        } else {
            if (up) s = s - 10;
            up = false;
            renko.push({
                time: getCustomDate(i),
                open: s,
                high: s,
                low: s - 10,
                close: s -10
            });  
            s = s - 10;
        }
    }
    return renko;
}
