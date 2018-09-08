package mass.job.web.route

import akka.http.scaladsl.server.Route
import mass.http.AbstractRoute
import mass.job.business.Services
import mass.job.web.route.api.ApiRoute

class Routes(services: Services) extends AbstractRoute {

  def route: Route =
    new ApiRoute(services).route

}
