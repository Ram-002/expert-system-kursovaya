refreshButton.click(() => {
    updateStocks();
    if (Stock.selectedStock != null) {
        displayPrices();
    }
});

//region Stock control
const stockSelector = $('#admin-stock-select');

/**
 * Text field for new stock name
 */
const stockAddNameInput = $('#admin-stock-add-name');

function updateStocks() {
    Stock.get().then(value => {
        stockSelector.empty();
        value.forEach(name => {
                stockSelector.append($(`
                    <div class="admin-stock-select" ">
                        <div onclick="Stock.selectedStock= '${name}'; displayPrices()">${name}</div>
                        <img src="img/x.svg" onclick="deleteStock('${name}')">
                    </div>
                `));
            }
        );
    }).catch(f => {
        alert(`Failed ${f}`);
    });
}

function deleteStock(name) {
    hidePrice();
    Stock.delete(name).then(() => {
        updateStocks();
    }).catch(() => {
        console.error("Can not delete stock");
    });
}

/**
 * Add stock
 */
$('#admin-stock-add-btn').click(() => {
    Stock.add(stockAddNameInput.val()).then(() => {
        updateStocks();
    }).catch(() => {
        console.error("Can not add stock");
    });
});
//endregion


//region Price control
const priceAddTitleDefault = $("#admin-price-add-title-default");
const priceAddTitle = $("#admin-price-add-title-name");
const priceAddTitleName = $("#admin-price-add-title-name-name");
const priceAddControl = $("#admin-price-add-control");
const priceDisplay = $("#admin-price-display");

function hidePrice() {
    priceDisplay.empty();
    priceAddTitleDefault.show();
    priceAddTitle.hide();
    priceAddControl.hide();
}

const stockPricesPanel = $("#admin-price-display");

hidePrice();

function displayPrices() {
    priceDisplay.empty();
    priceAddTitleDefault.hide();
    priceAddTitle.show()
    priceAddTitleName.text(Stock.selectedStock);
    priceAddControl.show();
    //TODO use GET
    //jquery does not allows GET requests to have body
    Stock.getPrice().then(prices => {
        prices.forEach(price => {
            let date = new Date(price['date'] * 86400000);
            stockPricesPanel.append($(`
                <div class="admin-price-display-row">
                    <div class="admin-price-display-date">День ${date.getUTCDate()}.${date.getUTCMonth() + 1}.${date.getUTCFullYear()}</div>
                    <div class="admin-price-display-price">Цена ${price['price'] / 100}</div>
                    <div class="admin-price-display-price">Дивиденды ${price['dividends'] / 100}</div>
                </div>
            `))
        })
    });
}

$("#price-add-btn").click(() => {
    let date = Math.floor(new Date($("#price-add-date").val()).getTime() / 86400000);
    let price = Number($("#price-add-price").val());
    let dividends = Number($("#price-add-dividends").val());
    Stock.addPrice(date, price, dividends).then(() => {
        displayPrices();
    });
});
//endregion

