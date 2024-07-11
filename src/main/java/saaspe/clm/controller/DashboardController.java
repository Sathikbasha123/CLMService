package saaspe.clm.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import saaspe.clm.model.CommonResponse;
import saaspe.clm.model.Response;
import saaspe.clm.service.DashboardService;

@RestController
@RequestMapping("api/v1/dashboard")
public class DashboardController {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

	@Autowired
	DashboardService dashboardService;

	@GetMapping("/view")
	public ResponseEntity<CommonResponse> getDashboardView(HttpServletRequest request) {
		try {
			CommonResponse dashboardview = dashboardService.getDashboardView(request);
			return ResponseEntity.status(HttpStatus.OK).body(dashboardview);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getDashboardView method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("pcd/loa/view")
	public ResponseEntity<CommonResponse> getPcdDashboard(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getPcdLoaDashboardCount(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("pcd/contract/view")
	public ResponseEntity<CommonResponse> getPcdContractDashboard(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getPcdContractDashboardCount(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("pcd/loa-onboarded-list")
	public ResponseEntity<CommonResponse> getLoaLatestOnboardedDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getLoaLatestOnboardedDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getLoaLatestOnboardedDashboardList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdLoaOnboardedList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("pcd/pending-review-list")
	public ResponseEntity<CommonResponse> getPcdPendingForReviewDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getPcdPendingForReviewDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdPendingReviewList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("pcd/contract-onboarded-list")
	public ResponseEntity<CommonResponse> getContractLatestOnboardedDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getContractLatestOnboardedDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdContractOnboardedList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("pcd/loa-expiring-list")
	public ResponseEntity<CommonResponse> getLoaExpiringDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getLoaExpiringDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdExpiringLoaList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("pcd/contract-expiring-list")
	public ResponseEntity<CommonResponse> getContractExpiringDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getContractExpiringDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdContractExpiringList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/cmu-onboarded-list")
	public ResponseEntity<CommonResponse> getCmuLatestOnboardedDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getCmuLatestOnboardedDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardCommercialCmuOnboardedLoaList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/cmu-expiring-list")
	public ResponseEntity<CommonResponse> getCmuExpiringDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getCmuExpiringDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdContractOnboardedLoaList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/loo-onboarded-list")
	public ResponseEntity<CommonResponse> getLooLatestOnboardedDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getLooLatestOnboardedDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardCommercialLooOnboardedLoaList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/loo-expiring-list")
	public ResponseEntity<CommonResponse> getLooExpiringDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getLooExpiringDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdContractOnboardedLoaList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/ta-onboarded-list")
	public ResponseEntity<CommonResponse> getTaLatestOnboardedDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getTaLatestOnboardedDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardCommercialTaOnboardedLoaList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/ta-expiring-list")
	public ResponseEntity<CommonResponse> getTaExpiringDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getTaExpiringDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardPcdContractOnboardedLoaList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/pending-review-list")
	public ResponseEntity<CommonResponse> getCommercialPendingForReviewDashboardList(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getCommercialPendingForReviewDashboardList(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getCommercialPendingForReviewDashboardList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardCommercialPendingReviewList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/cmu/view")
	public ResponseEntity<CommonResponse> getCommercialCmuDashboard(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getCommercialCmuDashboardCount(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/loo/view")
	public ResponseEntity<CommonResponse> getCommercialLooDashboard(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getCommercialLooDashboardCount(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("commercial/ta/view")
	public ResponseEntity<CommonResponse> getCommercialTaDashboard(HttpServletRequest request) {
		try {
			CommonResponse response = dashboardService.getCommercialTaDashboardCount(request);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getPcdDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("legal/contract/view")
	public ResponseEntity<CommonResponse> getLegalContractDashboard(HttpServletRequest request,@RequestParam(required = false,name = "category") String category,@RequestParam(required = false,name = "subsidiary") String subsidiary ) {
		try {
			CommonResponse response = dashboardService.getLegalContractDashboardCount(request,category,subsidiary);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getLegalContractDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("legal/ta/view")
	public ResponseEntity<CommonResponse> getLegalTaDashboard(HttpServletRequest request,@RequestParam(required = false,name = "category") String category,@RequestParam(required = false,name = "subsidiary") String subsidiary ) {
		try {
			CommonResponse response = dashboardService.getLegalTaDashboardCount(request,category,subsidiary);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getLegalTaDashboard method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardView", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("legal/contract-list")
	public ResponseEntity<CommonResponse> getContractCompletedCvefAndStamperDashboardList(HttpServletRequest request,@RequestParam(required = false,name = "category") String category,@RequestParam(required = false,name = "subsidiary") String subsidiary ) {
		try {
			CommonResponse response = dashboardService.getContractCompletedCvefAndStamperDashboardList(request,category,subsidiary);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getContractCompletedCvefAndStamperDashboardList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardLegalList", new ArrayList<>()), e.getMessage()));
		}
	}

	@GetMapping("legal/ta-list")
	public ResponseEntity<CommonResponse> getTaCompletedCvefAndStamperDashboardList(HttpServletRequest request,@RequestParam(required = false,name = "category") String category,@RequestParam(required = false,name = "subsidiary") String subsidiary ) {
		try {
			CommonResponse response = dashboardService.getTaCompletedCvefAndStamperDashboardList(request,category,subsidiary);
			return ResponseEntity.status(HttpStatus.OK).body(response);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("*** Ending getTaCompletedCvefAndStamperDashboardList method with an error ***", e);
			return ResponseEntity.badRequest().body(new CommonResponse(HttpStatus.BAD_REQUEST,
					new Response("DashboardLegalList", new ArrayList<>()), e.getMessage()));
		}
	}
}
