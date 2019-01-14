function MobLink()
{
	var route = null;	// 跳转路由
	var dealCB = null;

	this.setRoute = function(r)
	{
		route = r;
		if (dealCB) {
			dealCB(route);
		}
	}

	this.setDealCB = function(cb)
	{
		dealCB = cb;
		if (route) {
			dealCB(route);
		}
	}

	this.log = function()
	{
		console.log(route);
		console.log(dealCB);
	}

}
var $moblink = new MobLink();
if (window.moblinkInitRoute)
{
	$moblink.setRoute(window.moblinkInitRoute);
}
