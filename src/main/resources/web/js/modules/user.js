const userLogin = $("#user-login");
const userConsole = $("#user-console");

userConsole.hide();

refreshButton.click(() => {
    if (Portfolio.username != null) {
        refreshConsole();
        updateAnalytics();
    }
});


$("#user-login-btn").click(() => {
    Portfolio.username = $("#user-login-input").val();
    userLogin.hide();
    userConsole.show();
    refreshConsole();
    updateAnalytics();
});


//region Portfolio control
$("#user-console-portfolio-control-add").click(() => {
    Portfolio.add(
        Math.floor(new Date($("#user-console-portfolio-control-date").val()).getTime() / 86400000),
        $("#user-console-portfolio-control-select").val(),
        $("#user-console-portfolio-control-amount").val()
    ).then(() => {
        refreshConsole();
    });
});


const portfolioStockSelect = $("#user-console-portfolio-control-select");
const portfolioDisplay = $("#user-console-portfolio-display");


function refreshConsole() {
    portfolioStockSelect.empty();
    portfolioDisplay.empty();
    Stock.get().then(value => {
        value.forEach(name => {
            portfolioStockSelect.append($(`
                <option value="${name}">${name}</option>
            `));
        })
    });
    Portfolio.get().then(/**@type Object*/portfolio => {
        Object.keys(portfolio).forEach(name => {
            let group = $(`<div class="portfolio-category"><div class="portfolio-category-title">${name}</div></div>`);
            portfolioDisplay.append(group);
            Object.keys(portfolio[name]).forEach(rawDate => {
                date = new Date(rawDate * 86400000);
                group.append($(`
                    <div class="portfolio-category-body">
                        <div>Дата покупки: ${date.getUTCDate()}.${date.getUTCMonth() + 1}.${date.getUTCFullYear()}</div> 
                        <div>Количество: ${portfolio[name][rawDate]}</div>
                        <img src="img/x.svg" onclick="deletePortfolioEntry('${name}', ${rawDate})" alt="x">   
                    </div>
                `));
            })
        });
    })
}

function deletePortfolioEntry(stock, date) {
    Portfolio.remove(date, stock).then(() => {
        refreshConsole();
    });
}

//endregion


let priceChart = null;

function updateAnalytics() {
    Portfolio.analytics().then(value => {
        $("#user-console-calc-purchase").text(value.purchase ? value.purchase / 100 : 0);

        if (priceChart != null) {
            priceChart.destroy();
        }

        const portfolioPrice = [];
        const portfolioPriceLabels = [];
        Object.keys(value.portfolioPriceByDate).forEach(rawDate => {
            portfolioPrice.push(value.portfolioPriceByDate[rawDate] / 100);
            date = new Date(rawDate * 86400000)
            portfolioPriceLabels.push(`${date.getUTCDate()}.${date.getUTCMonth() + 1}.${date.getUTCFullYear()}`);
        });

        const datasetsByStock = [];
        Object.keys(value.byStockByDate).forEach(stockName => {
            let prices = [];
            Object.values(value.byStockByDate[stockName]).forEach(price => {
                prices.push(price / 100);
            });
            let r = Math.random() * 255;
            let g = Math.random() * 255;
            let b = Math.random() * 255;
            datasetsByStock.push({
                label: stockName,
                backgroundColor: `rgb(${r},${g},${b})`,
                borderColor: `rgb(${r},${g},${b})`,
                data: prices,
                fill: false,
                type: "line",
                tension: 0
            })
        });

        let horizontalLineData = new Array(portfolioPriceLabels.length)

        datasetsByStock.push({
            label: 'Стоимость покупки акций',
            backgroundColor: `rgb(226, 0, 57)`,
            borderColor: `rgb(226, 0, 57)`,
            data: new Array(portfolioPriceLabels.length).fill(value.purchase / 100),
            fill: false,
            type: "line",
            tension: 0,
            radius: 0
        });

        priceChart = new Chart($("#user-console-calc-price-chart")[0].getContext("2d"), {
            type: "line",
            data: {
                labels: portfolioPriceLabels,
                datasets: [{
                    label: "Стоимость портфеля",
                    backgroundColor: "rgb(31,255,122)",
                    borderColor: "rgb(31,255,122)",
                    data: portfolioPrice,
                    fill: false,
                    tension: 0
                }].concat(datasetsByStock)
            },
            options: {
                maintainAspectRatio: false,
                bezierCurve: false,
                scales: {
                    yAxes: [{
                        ticks: {
                            beginAtZero: true
                        }
                    }]
                }
            }
        });

    })
}

